package com.quickbite.tracking.dto;

import java.time.LocalDateTime;

public record TrackingResponse(
        String orderReference,
        String status,
        String deliveryAgent,
        String estimatedDeliveryWindow,
        String message,
        LocalDateTime updatedAt
) {
}

