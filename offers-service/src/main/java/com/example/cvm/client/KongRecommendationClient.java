package com.example.cvm.client;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import com.example.cvm.config.KongProperties;
import com.example.cvm.dto.KongRequest;
import com.example.cvm.dto.KongResponse;
import com.example.cvm.exception.KongErrors;
import com.example.cvm.exception.KongException;
import com.example.cvm.exception.TokenRejectedException;

@Component
public class KongRecommendationClient {

    private final RestClient restClient;
    private final KongProperties props;

    public KongRecommendationClient(RestClient kongRestClient, KongProperties props) {
        this.restClient = kongRestClient;
        this.props = props;
    }

    public KongResponse getRecommendations(String token, KongRequest request) {
        KongResponse response;
        try {
            response = restClient.post()
                    .uri(props.recommendationsPath())
                    .contentType(MediaType.APPLICATION_JSON)
                    .headers(h -> h.setBearerAuth(token))
                    .body(request)
                    .retrieve()
                    .body(KongResponse.class);
        } catch (HttpClientErrorException.Unauthorized e) {
            throw new TokenRejectedException(e);
        } catch (RestClientException e) {
            throw KongErrors.translate("Recommendations API", e);
        }
        if (response == null) {
            throw new KongException(HttpStatus.BAD_GATEWAY, "Recommendations API: empty response", null);
        }
        return response;
    }
}
