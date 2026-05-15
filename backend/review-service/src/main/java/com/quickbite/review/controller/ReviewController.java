package com.quickbite.review.controller;

import com.quickbite.review.dto.CreateReviewRequest;
import com.quickbite.review.dto.ReviewResponse;
import com.quickbite.review.service.ReviewService;
import jakarta.validation.Valid;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/reviews")
public class ReviewController {

    private static final Logger logger = LoggerFactory.getLogger(ReviewController.class);

    private final ReviewService reviewService;

    public ReviewController(ReviewService reviewService) {
        this.reviewService = reviewService;
    }

    @GetMapping("/restaurants/{restaurantId}")
    public List<ReviewResponse> getRestaurantReviews(@PathVariable Long restaurantId) {
        logger.debug("Fetching reviews for restaurantId={}", restaurantId);
        return reviewService.getRestaurantReviews(restaurantId);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ReviewResponse createReview(
            @RequestHeader("X-Authenticated-User") String userEmail,
            @Valid @RequestBody CreateReviewRequest request
    ) {
        logger.info("Create review request for restaurantId={} by user={}", request.restaurantId(), userEmail);
        return reviewService.createReview(userEmail, request);
    }
}
