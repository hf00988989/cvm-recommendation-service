package com.example.cvm.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record GetOffersRequest(
        @Schema(example = "628970196650")
        @NotBlank(message = "msisdn is required")
        @Pattern(regexp = "^\\d{10,15}$", message = "msisdn must be 10 to 15 digits")
        String msisdn) {
}
