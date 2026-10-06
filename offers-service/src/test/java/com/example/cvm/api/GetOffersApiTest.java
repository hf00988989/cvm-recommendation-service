package com.example.cvm.api;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import com.fasterxml.jackson.databind.JsonNode;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
class GetOffersApiTest {

    private static final HttpServer FAKE_KONG = startFakeKong();
    private static final AtomicInteger TOKEN_CALLS = new AtomicInteger();
    private static final AtomicInteger RECO_CALLS = new AtomicInteger();
    private static volatile String validToken = "token-1";
    private static volatile boolean rejectFirstReco;
    private static volatile int recoHttpStatus = 200;
    private static volatile String lastRecoBody;

    @Autowired
    private TestRestTemplate rest;

    @DynamicPropertySource
    static void props(DynamicPropertyRegistry r) {
        r.add("kong.base-url", () -> "http://localhost:" + FAKE_KONG.getAddress().getPort());
    }

    @AfterAll
    static void stop() {
        FAKE_KONG.stop(0);
    }

    @BeforeEach
    void reset() {
        TOKEN_CALLS.set(0);
        RECO_CALLS.set(0);
        rejectFirstReco = false;
        recoHttpStatus = 200;
        lastRecoBody = null;
    }

    @Test
    void happyPathTransformsKongResponse() {
        ResponseEntity<JsonNode> res = rest.postForEntity("/api/v1/get-offers",
                new Req("628970196650"), JsonNode.class);

        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.OK);
        JsonNode body = res.getBody();
        assertThat(body.get("status").asBoolean()).isTrue();
        assertThat(body.get("code").asInt()).isEqualTo(1);
        assertThat(body.get("data").isArray()).isTrue();
        assertThat(body.get("data")).hasSize(2);
        assertThat(body.get("data").get(0).get("offerId").asText()).isEqualTo("234589222");
        assertThat(body.get("data").get(0).get("Description").asText()).startsWith("100% Kuota Utama");

        assertThat(lastRecoBody).contains("\"msisdn\":\"628970196650\"")
                .contains("\"lang\":\"EN\"").contains("\"channel\":\"3id\"").contains("\"transid\":\"");
    }

    @Test
    void tokenIsCachedAcrossCalls() {
        rest.postForEntity("/api/v1/get-offers", new Req("628970196650"), JsonNode.class);
        int tokenCallsAfterFirst = TOKEN_CALLS.get();
        rest.postForEntity("/api/v1/get-offers", new Req("628970196651"), JsonNode.class);

        assertThat(TOKEN_CALLS.get()).isEqualTo(tokenCallsAfterFirst);
    }

    @Test
    void refreshesTokenAndRetriesOnceOn401() {
        rejectFirstReco = true;
        ResponseEntity<JsonNode> res = rest.postForEntity("/api/v1/get-offers",
                new Req("628970196650"), JsonNode.class);

        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(RECO_CALLS.get()).isEqualTo(2);
    }

    @Test
    void invalidMsisdnReturns400() {
        ResponseEntity<JsonNode> res = rest.postForEntity("/api/v1/get-offers", new Req("abc"), JsonNode.class);

        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(res.getBody().get("status").asBoolean()).isFalse();
        assertThat(res.getBody().get("code").asInt()).isZero();
    }

    @Test
    void kongServerErrorReturns502() {
        recoHttpStatus = 500;
        ResponseEntity<JsonNode> res = rest.postForEntity("/api/v1/get-offers",
                new Req("628970196650"), JsonNode.class);

        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.BAD_GATEWAY);
        assertThat(res.getBody().get("status").asBoolean()).isFalse();
    }

    record Req(String msisdn) {
    }

    private static HttpServer startFakeKong() {
        try {
            HttpServer server = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
            server.createContext("/oauth/token", ex -> {
                TOKEN_CALLS.incrementAndGet();
                validToken = "token-" + TOKEN_CALLS.get();
                respond(ex, 200, "{\"access_token\":\"" + validToken + "\",\"token_type\":\"Bearer\",\"expires_in\":300}");
            });
            server.createContext("/offers/recommendations", ex -> {
                int call = RECO_CALLS.incrementAndGet();
                lastRecoBody = new String(ex.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
                String auth = ex.getRequestHeaders().getFirst("Authorization");
                if (rejectFirstReco && call == 1) {
                    respond(ex, 401, "{\"message\":\"Unauthorized\"}");
                } else if (!("Bearer " + validToken).equals(auth)) {
                    respond(ex, 401, "{\"message\":\"Unauthorized\"}");
                } else if (recoHttpStatus != 200) {
                    respond(ex, recoHttpStatus, "{}");
                } else {
                    try (InputStream in = GetOffersApiTest.class.getResourceAsStream("/kong-response.json")) {
                        respond(ex, 200, new String(in.readAllBytes(), StandardCharsets.UTF_8));
                    }
                }
            });
            server.start();
            return server;
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    private static void respond(HttpExchange ex, int status, String body) throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        ex.getResponseHeaders().add("Content-Type", "application/json");
        ex.sendResponseHeaders(status, bytes.length);
        ex.getResponseBody().write(bytes);
        ex.close();
    }
}
