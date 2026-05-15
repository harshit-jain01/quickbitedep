package com.quickbite.restaurant.dto;

import java.time.LocalDateTime;
import java.util.UUID;

public record AdminRestaurantOwnerResponse(
        Long ownerId,
        UUID userId,
        String restaurantName,
        String cuisineType,
        String address,
        String email,
        String phone,
        boolean active,
        LocalDateTime createdAt
) {
        public Long id() {
                return ownerId;
        }
}

