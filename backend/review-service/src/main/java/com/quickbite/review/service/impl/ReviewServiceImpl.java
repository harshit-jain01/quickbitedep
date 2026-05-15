package com.quickbite.review.service.impl;

import com.quickbite.review.dto.CreateReviewRequest;
import com.quickbite.review.dto.ReviewResponse;
import com.quickbite.review.model.Review;
import com.quickbite.review.repository.ReviewRepository;
import com.quickbite.review.service.ReviewService;
import java.time.Instant;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ReviewServiceImpl implements ReviewService {

    private static final Logger logger = LoggerFactory.getLogger(ReviewServiceImpl.class);

    private final ReviewRepository reviewRepository;

    public ReviewServiceImpl(ReviewRepository reviewRepository) {
        this.reviewRepository = reviewRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public List<ReviewResponse> getRestaurantReviews(Long restaurantId) {
        logger.debug("Loading review list for restaurantId={}", restaurantId);
        return reviewRepository.findByRestaurantIdOrderByCreatedAtDesc(restaurantId).stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    @Transactional
    public ReviewResponse createReview(String userEmail, CreateReviewRequest request) {
        logger.info("Persisting review for restaurantId={} from user={}", request.restaurantId(), userEmail);
        String reviewerName = userEmail.contains("@") ? userEmail.substring(0, userEmail.indexOf('@')) : userEmail;
        Review review = new Review();
        review.setRestaurantId(request.restaurantId());
        review.setReviewerName(reviewerName);
        review.setRating(request.rating());
        review.setComment(request.comment());
        review.setCreatedAt(Instant.now());
        return toResponse(reviewRepository.save(review));
    }

    private ReviewResponse toResponse(Review review) {
        return new ReviewResponse(
                review.getId(),
                review.getRestaurantId(),
                review.getReviewerName(),
                review.getRating(),
                review.getComment(),
                review.getCreatedAt()
        );
    }
}

