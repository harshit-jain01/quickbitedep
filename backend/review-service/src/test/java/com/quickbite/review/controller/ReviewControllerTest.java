package com.quickbite.review.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.quickbite.review.dto.ReviewResponse;
import com.quickbite.review.exception.GlobalExceptionHandler;
import com.quickbite.review.service.ReviewService;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(controllers = ReviewController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class ReviewControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ReviewService reviewService;

    @Test
    void getRestaurantReviews_shouldReturn200() throws Exception {
        when(reviewService.getRestaurantReviews(1L)).thenReturn(List.of(
                new ReviewResponse(1L, 1L, "Aditi", 5, "Great", Instant.now())
        ));

        mockMvc.perform(get("/api/v1/reviews/restaurants/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].reviewerName").value("Aditi"));

        verify(reviewService).getRestaurantReviews(1L);
    }

    @Test
    void createReview_shouldReturn201_whenValid() throws Exception {
        when(reviewService.createReview(eq("user@mail.com"), any())).thenReturn(
                new ReviewResponse(2L, 1L, "user", 4, "Nice", Instant.now())
        );

        mockMvc.perform(post("/api/v1/reviews")
                        .header("X-Authenticated-User", "user@mail.com")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "restaurantId": 1,
                                  "rating": 4,
                                  "comment": "Nice"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.rating").value(4));

        verify(reviewService).createReview(eq("user@mail.com"), any());
    }

    @Test
    void createReview_shouldReturn400_whenValidationFails() throws Exception {
        mockMvc.perform(post("/api/v1/reviews")
                        .header("X-Authenticated-User", "user@mail.com")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "restaurantId": 1,
                                  "rating": 10,
                                  "comment": ""
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").exists());
    }
}

