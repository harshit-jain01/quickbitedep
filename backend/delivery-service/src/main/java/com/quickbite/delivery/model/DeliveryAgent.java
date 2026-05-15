package com.quickbite.delivery.model;

import java.time.Instant;

public record DeliveryAgent(
        String id,
        String name,
        String phone,
        String vehicleType,
        String vehicleNumber,
        boolean verified,
        boolean active,
        boolean online,
        String currentOrderReference,
        int completedDeliveries,
        Instant createdAt
) {

    public DeliveryAgent withVerification(boolean isVerified, boolean isActive) {
        return new DeliveryAgent(
                id,
                name,
                phone,
                vehicleType,
                vehicleNumber,
                isVerified,
                isActive,
                online,
                currentOrderReference,
                completedDeliveries,
                createdAt
        );
    }

    public DeliveryAgent withAvailability(boolean isOnline) {
        return new DeliveryAgent(
                id,
                name,
                phone,
                vehicleType,
                vehicleNumber,
                verified,
                active,
                isOnline,
                currentOrderReference,
                completedDeliveries,
                createdAt
        );
    }

    public DeliveryAgent withCurrentOrder(String orderReference) {
        return new DeliveryAgent(
                id,
                name,
                phone,
                vehicleType,
                vehicleNumber,
                verified,
                active,
                online,
                orderReference,
                completedDeliveries,
                createdAt
        );
    }

    public DeliveryAgent incrementCompletedDeliveries() {
        return new DeliveryAgent(
                id,
                name,
                phone,
                vehicleType,
                vehicleNumber,
                verified,
                active,
                online,
                null,
                completedDeliveries + 1,
                createdAt
        );
    }
}

