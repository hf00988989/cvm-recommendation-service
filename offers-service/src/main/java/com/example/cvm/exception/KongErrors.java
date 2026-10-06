package com.example.cvm.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClientException;

public final class KongErrors {

    private KongErrors() {
    }

    public static KongException translate(String operation, RestClientException e) {
        if (e instanceof ResourceAccessException) {
            return new KongException(HttpStatus.GATEWAY_TIMEOUT, operation + ": Kong unreachable or timed out", e);
        }
        return new KongException(HttpStatus.BAD_GATEWAY, operation + ": Kong returned an error", e);
    }
}
