package com.example.cvm.exception;

/** Kong rejected the bearer token (HTTP 401). */
public class TokenRejectedException extends RuntimeException {

    public TokenRejectedException(Throwable cause) {
        super("Kong rejected the access token", cause);
    }
}
