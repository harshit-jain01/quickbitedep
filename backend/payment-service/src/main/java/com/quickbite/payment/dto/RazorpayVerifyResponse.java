package com.quickbite.payment.dto;

public record RazorpayVerifyResponse(
        String orderReference,
        String paymentMode,
        String paymentStatus,
        String razorpayOrderId,
        String razorpayPaymentId,
        boolean verified,
        String message
) {
}

