package com.quickbite.review.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.quickbite.review.dto.CreateReviewRequest;
import com.quickbite.review.model.Review;
import com.quickbite.review.repository.ReviewRepository;
import com.quickbite.review.service.impl.ReviewServiceImpl;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ReviewServiceTest {

    @Mock
    private ReviewRepository reviewRepository;

    @InjectMocks
    private ReviewServiceImpl reviewService;

    @Test
    void getRestaurantReviews_shouldMapRepositoryRows() {
        Review review = new Review();
        review.setId(10L);
        review.setRestaurantId(1L);
        review.setReviewerName("Aditi");
        review.setRating(5);
        review.setComment("Great");
        review.setCreatedAt(Instant.now());
        when(reviewRepository.findByRestaurantIdOrderByCreatedAtDesc(1L)).thenReturn(List.of(review));

        var result = reviewService.getRestaurantReviews(1L);

        assertEquals(1, result.size());
        assertEquals("Aditi", result.get(0).reviewerName());
    }

    @Test
    void createReview_shouldDeriveReviewerNameFromEmail() {
        CreateReviewRequest request = new CreateReviewRequest(1L, 4, "Nice food");
        Review savedReview = new Review();
        savedReview.setId(11L);
        savedReview.setRestaurantId(1L);
        savedReview.setReviewerName("john");
        savedReview.setRating(4);
        savedReview.setComment("Nice food");
        savedReview.setCreatedAt(Instant.now());
        when(reviewRepository.save(any(Review.class))).thenReturn(savedReview);

        var response = reviewService.createReview("john@mail.com", request);

        assertEquals("john", response.reviewerName());
        verify(reviewRepository).save(any(Review.class));
    }
}

