package com.quickbite.order.dto.external;

public record PaymentRequest(
        String orderReference,
        String customerEmail,
        String paymentMethod,
        double amount
) {
}
