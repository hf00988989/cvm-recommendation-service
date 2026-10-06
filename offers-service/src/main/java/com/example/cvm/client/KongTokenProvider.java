package com.example.cvm.client;

import java.time.Clock;
import java.time.Instant;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import com.example.cvm.config.KongProperties;
import com.example.cvm.dto.KongTokenResponse;
import com.example.cvm.exception.KongErrors;
import com.example.cvm.exception.KongException;

/** Fetches an OAuth2 client-credentials token from Kong and caches it until shortly before expiry. */
@Component
public class KongTokenProvider {

    private static final long DEFAULT_TTL_SECONDS = 300;

    private final RestClient restClient;
    private final KongProperties props;
    private final Clock clock;

    private String token;
    private Instant expiresAt = Instant.EPOCH;

    public KongTokenProvider(RestClient kongRestClient, KongProperties props, Clock clock) {
        this.restClient = kongRestClient;
        this.props = props;
        this.clock = clock;
    }

    public synchronized String getToken() {
        if (token == null || !clock.instant().isBefore(expiresAt)) {
            fetch();
        }
        return token;
    }

    public synchronized void invalidate() {
        token = null;
        expiresAt = Instant.EPOCH;
    }

    private void fetch() {
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("grant_type", "client_credentials");
        form.add("client_id", props.clientId());
        form.add("client_secret", props.clientSecret());

        KongTokenResponse response;
        try {
            response = restClient.post()
                    .uri(props.tokenPath())
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(form)
                    .retrieve()
                    .body(KongTokenResponse.class);
        } catch (RestClientException e) {
            throw KongErrors.translate("Token API", e);
        }
        if (response == null || response.accessToken() == null || response.accessToken().isBlank()) {
            throw new KongException(HttpStatus.BAD_GATEWAY, "Token API: empty token in response", null);
        }
        long ttl = response.expiresIn() != null ? response.expiresIn() : DEFAULT_TTL_SECONDS;
        token = response.accessToken();
        expiresAt = clock.instant().plusSeconds(Math.max(0, ttl - props.tokenExpirySkewSeconds()));
    }
}
