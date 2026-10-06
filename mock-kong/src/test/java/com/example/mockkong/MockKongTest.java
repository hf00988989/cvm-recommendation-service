package com.example.mockkong;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import com.fasterxml.jackson.databind.JsonNode;

@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
class MockKongTest {

    @Autowired
    private TestRestTemplate rest;

    private String fetchToken(String secret) {
        HttpHeaders h = new HttpHeaders();
        h.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("grant_type", "client_credentials");
        form.add("client_id", "demo-client");
        form.add("client_secret", secret);
        ResponseEntity<JsonNode> res = rest.postForEntity("/oauth/token", new HttpEntity<>(form, h), JsonNode.class);
        return res.getStatusCode().is2xxSuccessful() ? res.getBody().get("access_token").asText() : null;
    }

    @Test
    void wrongSecretIsRejected() {
        assertThat(fetchToken("nope")).isNull();
    }

    @Test
    void recommendationsRequireValidToken() {
        String body = "{\"transid\":\"1\",\"msisdn\":\"628970196650\"}";
        HttpHeaders h = new HttpHeaders();
        h.setContentType(MediaType.APPLICATION_JSON);

        ResponseEntity<String> noAuth = rest.postForEntity("/offers/recommendations", new HttpEntity<>(body, h), String.class);
        assertThat(noAuth.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);

        h.setBearerAuth(fetchToken("demo-secret"));
        ResponseEntity<JsonNode> ok = rest.postForEntity("/offers/recommendations", new HttpEntity<>(body, h), JsonNode.class);
        assertThat(ok.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(ok.getBody().at("/offers/recharge-offer")).hasSize(2);
    }
}
