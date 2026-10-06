package com.example.cvm.dto;

import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public record KongResponse(Status status, Offers offers) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Status(Integer code, String message) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Offers(@JsonProperty("recharge-offer") List<KongOffer> rechargeOffer) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record KongOffer(
            String id,
            String shortMessage,
            @JsonProperty("additional-params") Map<String, List<String>> additionalParams,
            Integer order,
            Long price) {
    }
}
