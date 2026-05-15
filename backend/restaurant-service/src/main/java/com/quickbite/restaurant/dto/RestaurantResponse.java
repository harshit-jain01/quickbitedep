package com.quickbite.restaurant.dto;

import java.util.List;

public record RestaurantResponse(
        Long id,
        String name,
        String category,
        String area,
        List<String> cuisines,
        double rating,
        int deliveryTimeMinutes,
        int priceForTwo,
        double distanceKm,
        String imageUrl,
        String description
) {
}
