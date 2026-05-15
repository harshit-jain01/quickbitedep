package com.quickbite.order.dto.external;

public record PaymentResponse(
        String transactionId,
        String orderReference,
        String paymentMethod,
        double amount,
        String status
) {
}
