package com.quickbite.restaurant.service;

import com.quickbite.restaurant.dto.AdminRestaurantOwnerResponse;
import com.quickbite.restaurant.dto.RestaurantOwnerProfileResponse;
import com.quickbite.restaurant.model.RestaurantOwnerEntity;
import org.springframework.web.multipart.MultipartFile;
import java.util.List;
import java.util.UUID;
public interface RestaurantOwnerService {

    RestaurantOwnerProfileResponse getRestaurantOwnerProfile(UUID ownerId);

    RestaurantOwnerProfileResponse updateRestaurantProfile(UUID ownerId, RestaurantOwnerProfileResponse request);

    RestaurantOwnerEntity createRestaurantOwner(UUID userId, String restaurantName, String cuisineType, String address, MultipartFile imageFile);

    List<AdminRestaurantOwnerResponse> getAllOwnersForAdmin();
}

