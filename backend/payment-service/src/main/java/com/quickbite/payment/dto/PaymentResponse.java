package com.quickbite.payment.dto;

public record PaymentResponse(
        String transactionId,
        String orderReference,
        String paymentMethod,
        double amount,
        String status
) {
}
