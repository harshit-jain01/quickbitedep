package com.quickbite.restaurant.model;

import java.util.List;

public record Restaurant(
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
