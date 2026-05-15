package com.quickbite.menu.model;

public record MenuItem(
        Long id,
        Long restaurantId,
        String name,
        String description,
        String category,
        double price,
        boolean vegetarian,
        boolean bestseller,
        String imageUrl
) {
}
