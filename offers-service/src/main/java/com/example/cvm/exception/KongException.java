package com.example.cvm.exception;

import org.springframework.http.HttpStatus;

public class KongException extends RuntimeException {

    private final HttpStatus httpStatus;

    public KongException(HttpStatus httpStatus, String message, Throwable cause) {
        super(message, cause);
        this.httpStatus = httpStatus;
    }

    public HttpStatus getHttpStatus() {
        return httpStatus;
    }
}
