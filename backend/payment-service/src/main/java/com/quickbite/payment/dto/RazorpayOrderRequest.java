package com.quickbite.payment.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record RazorpayOrderRequest(
        @NotBlank String orderReference,
        @NotBlank String customerEmail,
        @NotBlank String paymentMode,
        @NotNull @DecimalMin(value = "1.0", inclusive = true) Double amount,
        @NotBlank String currency,
        String notes
) {
}

