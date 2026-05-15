package com.quickbite.order.dto;

import java.time.Instant;
import java.util.List;

public record OrderResponse(
        String orderReference,
        String displayOrderId,
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
        String estimatedDeliveryWindow,
        double total,
        String notes,
        Instant createdAt,
        List<OrderItemResponse> items
) {
}
