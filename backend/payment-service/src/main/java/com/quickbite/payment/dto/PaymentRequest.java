package com.quickbite.payment.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record PaymentRequest(
        @NotBlank String orderReference,
        @NotBlank String customerEmail,
        @NotBlank String paymentMethod,
        @NotNull Double amount
) {
}
