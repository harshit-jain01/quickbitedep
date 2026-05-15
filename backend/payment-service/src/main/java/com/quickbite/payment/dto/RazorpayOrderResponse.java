package com.quickbite.payment.dto;

public record RazorpayOrderResponse(
        String keyId,
        String orderReference,
        String razorpayOrderId,
        long amount,
        String currency,
        String paymentMode,
        String status
) {
}

