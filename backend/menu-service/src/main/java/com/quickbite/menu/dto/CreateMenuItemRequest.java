package com.quickbite.menu.dto;

public record CreateMenuItemRequest(
        Long restaurantId,
        Long categoryId,
        String itemName,
        String description,
        Double price,
        String imageUrl,
        Boolean isAvailable,
        Boolean vegetarian,
        Boolean bestseller
) {
}
