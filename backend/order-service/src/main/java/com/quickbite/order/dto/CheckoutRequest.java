package com.quickbite.order.dto;

import jakarta.validation.constraints.NotBlank;

public record CheckoutRequest(
        @NotBlank String deliveryAddress,
        @NotBlank String paymentMethod,
        String notes,
        String orderReference,
        String razorpayOrderId,
        String razorpayPaymentId
) {
}
