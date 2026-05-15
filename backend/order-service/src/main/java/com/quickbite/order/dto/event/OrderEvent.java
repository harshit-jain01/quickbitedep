package com.quickbite.order.dto.event;

import java.time.Instant;

public record OrderEvent(
        String eventId,
        String eventType,
        String orderReference,
        String customerEmail,
        String restaurantName,
        String deliveryAddress,
        String paymentMode,
        String paymentStatus,
        Double totalAmount,
        Instant createdAt
) {
}

