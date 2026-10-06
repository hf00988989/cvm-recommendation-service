package com.example.cvm.dto;

import java.util.List;

public record GetOffersResponse(boolean status, String message, int code, List<OfferDto> data) {

    public static final int CODE_SUCCESS = 1;
    public static final int CODE_FAILURE = 0;

    public static GetOffersResponse success(List<OfferDto> offers) {
        return new GetOffersResponse(true, "Success", CODE_SUCCESS, offers);
    }

    public static GetOffersResponse failure(String message) {
        return new GetOffersResponse(false, message, CODE_FAILURE, List.of());
    }
}
