package com.example.cvm.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "kong")
public record KongProperties(
        String baseUrl,
        String tokenPath,
        String recommendationsPath,
        String clientId,
        String clientSecret,
        String lang,
        String channel,
        int connectTimeoutMs,
        int readTimeoutMs,
        long tokenExpirySkewSeconds) {
}
