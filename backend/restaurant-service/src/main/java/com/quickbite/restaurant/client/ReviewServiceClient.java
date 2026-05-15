package com.quickbite.restaurant.client;

import java.util.List;
import java.util.Map;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "review-service")
public interface ReviewServiceClient {

    @GetMapping("/api/v1/reviews/restaurants/{restaurantId}")
    List<Map<String, Object>> getRestaurantReviews(@PathVariable("restaurantId") Long restaurantId);
}
