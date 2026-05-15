package com.quickbite.tracking.dto.external;

public record OrderStatusPayload(
        String orderReference,
        String deliveryStatus,
        String deliveryAgent,
        int etaMinutes,
        String estimatedDeliveryWindow
) {
}

