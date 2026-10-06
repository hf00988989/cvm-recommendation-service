package com.example.cvm.dto;

public record KongRequest(String transid, String msisdn, ServiceData servicedata) {

    public record ServiceData(String lang, String channel) {
    }
}
