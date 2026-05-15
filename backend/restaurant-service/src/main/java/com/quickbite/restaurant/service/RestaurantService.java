package com.quickbite.restaurant.service;

import com.quickbite.restaurant.dto.CategoryResponse;
import com.quickbite.restaurant.dto.RestaurantResponse;
import java.util.List;
public interface RestaurantService {

    List<CategoryResponse> getCategories();

    List<RestaurantResponse> getRestaurants(String category, String search);

    RestaurantResponse getRestaurant(Long restaurantId);

    List<RestaurantResponse> getAllRestaurantsForAdmin();

    void deleteRestaurantById(Long restaurantId);
}
