package com.quickbite.payment.dto;

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

