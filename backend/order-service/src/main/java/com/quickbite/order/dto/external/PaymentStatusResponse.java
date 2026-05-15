package com.quickbite.order.dto.external;

public record PaymentStatusResponse(
        String orderReference,
        String paymentMode,
        String paymentStatus,
        String razorpayOrderId,
        String razorpayPaymentId,
        String currency,
        double amount
) {
}

