package com.example.cvm.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record OfferDto(
        String offerId,
        String offerShortDesc,
        String registrationKey,
        String validity,
        @JsonProperty("Description") String description,
        String retailerIncentive,
        String starred,
        String bestDeal,
        String cashbackValue,
        Integer order,
        Long price) {
}
