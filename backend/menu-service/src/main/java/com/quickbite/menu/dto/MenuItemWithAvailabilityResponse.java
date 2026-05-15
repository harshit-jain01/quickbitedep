package com.quickbite.menu.dto;

public record MenuItemWithAvailabilityResponse(
        Long id,
        Long restaurantId,
        String name,
        String description,
        String category,
        double price,
        boolean vegetarian,
        boolean bestseller,
        String imageUrl,
        boolean available,
        long createdAt,
        long updatedAt
) {
}

