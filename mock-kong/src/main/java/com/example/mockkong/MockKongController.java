package com.example.mockkong;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class MockKongController {

    private final String clientId;
    private final String clientSecret;
    private final long ttlSeconds;
    private final String recommendationsJson;
    private final Map<String, Instant> tokens = new ConcurrentHashMap<>();

    public MockKongController(@Value("${mock.client-id}") String clientId,
                              @Value("${mock.client-secret}") String clientSecret,
                              @Value("${mock.token-ttl-seconds}") long ttlSeconds) throws IOException {
        this.clientId = clientId;
        this.clientSecret = clientSecret;
        this.ttlSeconds = ttlSeconds;
        try (InputStream in = getClass().getResourceAsStream("/recommendations.json")) {
            this.recommendationsJson = new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    @PostMapping(value = "/oauth/token", consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Map<String, Object>> token(@RequestParam("client_id") String id,
                                                     @RequestParam("client_secret") String secret,
                                                     @RequestParam("grant_type") String grantType) {
        if (!"client_credentials".equals(grantType)) {
            return ResponseEntity.badRequest().body(Map.of("error", "unsupported_grant_type"));
        }
        if (!clientId.equals(id) || !clientSecret.equals(secret)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "invalid_client"));
        }
        String token = UUID.randomUUID().toString();
        tokens.put(token, Instant.now().plusSeconds(ttlSeconds));
        return ResponseEntity.ok(Map.of(
                "access_token", token,
                "token_type", "Bearer",
                "expires_in", ttlSeconds));
    }

    @PostMapping(value = "/offers/recommendations", consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> recommendations(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @RequestBody Map<String, Object> body) {
        if (!isValid(authorization)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body("{\"message\":\"Invalid or expired token\"}");
        }
        if (isBlank(body.get("transid")) || isBlank(body.get("msisdn"))) {
            return ResponseEntity.badRequest().body("{\"message\":\"transid and msisdn are required\"}");
        }
        return ResponseEntity.ok(recommendationsJson);
    }

    private boolean isValid(String authorization) {
        if (authorization == null || !authorization.startsWith("Bearer ")) {
            return false;
        }
        Instant expiry = tokens.get(authorization.substring(7));
        return expiry != null && Instant.now().isBefore(expiry);
    }

    private static boolean isBlank(Object value) {
        return value == null || value.toString().isBlank();
    }
}
