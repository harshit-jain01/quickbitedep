package com.quickbite.restaurant.dto;

import java.util.UUID;

public record RestaurantOwnerProfileResponse(
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

