package com.quickbite.delivery.dto;

public record DeliveryAgentResponse(
        String id,
        String name,
        String phone,
        String vehicleType,
        String vehicleNumber,
        boolean verified,
        boolean active,
        boolean online,
        int completedDeliveries
) {
}

