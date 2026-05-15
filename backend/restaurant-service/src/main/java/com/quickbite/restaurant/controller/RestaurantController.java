package com.quickbite.restaurant.controller;

import com.quickbite.restaurant.dto.CategoryResponse;
import com.quickbite.restaurant.dto.RestaurantResponse;
import com.quickbite.restaurant.service.RestaurantService;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/restaurants")
public class RestaurantController {

    private static final Logger logger = LoggerFactory.getLogger(RestaurantController.class);

    private final RestaurantService restaurantService;

    public RestaurantController(RestaurantService restaurantService) {
        this.restaurantService = restaurantService;
    }

    @GetMapping("/categories")
    public List<CategoryResponse> getCategories() {
        logger.debug("Restaurant categories requested");
        return restaurantService.getCategories();
    }

    @GetMapping
    public List<RestaurantResponse> getRestaurants(
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String search
    ) {
        logger.debug("Restaurant list requested category={} search={}", category, search);
        return restaurantService.getRestaurants(category, search);
    }

    @GetMapping("/{restaurantId}")
    public RestaurantResponse getRestaurant(@PathVariable Long restaurantId) {
        logger.info("Restaurant details requested for restaurantId={}", restaurantId);
        return restaurantService.getRestaurant(restaurantId);
    }
}
