package com.quickbite.review.service;

import com.quickbite.review.dto.CreateReviewRequest;
import com.quickbite.review.dto.ReviewResponse;
import java.util.List;
public interface ReviewService {

    List<ReviewResponse> getRestaurantReviews(Long restaurantId);

    ReviewResponse createReview(String userEmail, CreateReviewRequest request);
}
