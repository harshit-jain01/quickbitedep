package com.quickbite.review.dto;

import java.time.Instant;

public record ReviewResponse(
        Long id,
        Long restaurantId,
        String reviewerName,
        int rating,
        String comment,
        Instant createdAt
) {
}
