package com.quickbite.order.model;

import java.time.Instant;
import java.util.List;

public record Order(
        String orderReference,
        String userEmail,
        String restaurantName,
        String deliveryAddress,
        String paymentMode,
        String paymentStatus,
        String razorpayOrderId,
        String razorpayPaymentId,
        String deliveryStatus,
        String deliveryAgentId,
        String deliveryAgent,
        int etaMinutes,
        double total,
        String notes,
        Instant createdAt,
        List<OrderItem> items
) {
}
