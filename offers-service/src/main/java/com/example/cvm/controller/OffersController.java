package com.example.cvm.controller;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.cvm.dto.GetOffersRequest;
import com.example.cvm.dto.GetOffersResponse;
import com.example.cvm.service.OffersService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1")
@Tag(name = "Offers")
public class OffersController {

    private final OffersService offersService;

    public OffersController(OffersService offersService) {
        this.offersService = offersService;
    }

    @Operation(summary = "Get recharge offers for an MSISDN",
            description = "Gets a Kong token, calls the Kong recommendations API and transforms the result")
    @PostMapping(value = "/get-offers", consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public GetOffersResponse getOffers(@Valid @RequestBody GetOffersRequest request) {
        return offersService.getOffers(request.msisdn());
    }
}
