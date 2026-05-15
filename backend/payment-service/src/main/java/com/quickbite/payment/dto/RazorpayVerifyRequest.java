package com.quickbite.payment.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record RazorpayVerifyRequest(
        @NotBlank String orderReference,
        @NotBlank String customerEmail,
        @NotBlank String paymentMode,
        @NotNull @DecimalMin(value = "1.0", inclusive = true) Double amount,
        @NotBlank String currency,
        @NotBlank String razorpayOrderId,
        @NotBlank String razorpayPaymentId,
        @NotBlank String razorpaySignature
) {
}

