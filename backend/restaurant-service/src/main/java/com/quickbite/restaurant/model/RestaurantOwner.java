package com.quickbite.restaurant.model;

import java.util.UUID;

public record RestaurantOwner(
        Long restaurantId,
        UUID ownerId,
        String restaurantName,
        String cuisineType,
        String area,
        String description,
        String imageUrl,
        double minimumOrderAmount,
        int estimatedDeliveryTime,
        boolean isActive
) {
}

