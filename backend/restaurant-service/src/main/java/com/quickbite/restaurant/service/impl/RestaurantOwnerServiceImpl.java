package com.quickbite.restaurant.service.impl;

import com.quickbite.restaurant.dto.AdminRestaurantOwnerResponse;
import com.quickbite.restaurant.dto.RestaurantOwnerProfileResponse;
import com.quickbite.restaurant.model.RestaurantOwnerEntity;
import com.quickbite.restaurant.repository.RestaurantOwnerRepository;
import com.quickbite.restaurant.service.CloudinaryImageService;
import com.quickbite.restaurant.service.RestaurantOwnerService;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class RestaurantOwnerServiceImpl implements RestaurantOwnerService {

    private static final Logger logger = LoggerFactory.getLogger(RestaurantOwnerServiceImpl.class);

    private final RestaurantOwnerRepository restaurantOwnerRepository;
    private final CloudinaryImageService cloudinaryImageService;

    public RestaurantOwnerServiceImpl(RestaurantOwnerRepository restaurantOwnerRepository, CloudinaryImageService cloudinaryImageService) {
        this.restaurantOwnerRepository = restaurantOwnerRepository;
        this.cloudinaryImageService = cloudinaryImageService;
    }

    @Override
    public RestaurantOwnerProfileResponse getRestaurantOwnerProfile(UUID ownerId) {
        logger.debug("Loading restaurant owner profile for ownerId={}", ownerId);
        RestaurantOwnerEntity owner = restaurantOwnerRepository.findByUserId(ownerId)
                .orElseThrow(() -> new IllegalArgumentException("Restaurant owner not found for userId: " + ownerId));

        return new RestaurantOwnerProfileResponse(
                owner.getId(),
                owner.getUserId(),
                owner.getRestaurantName(),
                owner.getCuisineType(),
                owner.getCity() != null ? owner.getCity() : owner.getAddress(),
                owner.getAddress(),
                owner.getImageUrl(),
                owner.getMinimumOrderAmount() != null ? owner.getMinimumOrderAmount() : 0.0,
                owner.getEstimatedDeliveryTime() != null ? owner.getEstimatedDeliveryTime() : 30,
                owner.getIsActive() != null ? owner.getIsActive() : true
        );
    }

    @Override
    public RestaurantOwnerProfileResponse updateRestaurantProfile(UUID ownerId, RestaurantOwnerProfileResponse request) {
        logger.info("Updating restaurant owner profile for ownerId={}", ownerId);
        RestaurantOwnerEntity owner = restaurantOwnerRepository.findByUserId(ownerId)
                .orElseThrow(() -> new IllegalArgumentException("Restaurant owner not found for userId: " + ownerId));

        owner.setRestaurantName(request.restaurantName());
        owner.setCuisineType(request.cuisineType());
        owner.setAddress(request.description());
        owner.setCity(request.area());
        owner.setImageUrl(request.imageUrl());
        owner.setMinimumOrderAmount(request.minimumOrderAmount());
        owner.setEstimatedDeliveryTime(request.estimatedDeliveryTime());
        owner.setIsActive(request.isActive());
        owner.setUpdatedAt(LocalDateTime.now());

        RestaurantOwnerEntity updated = restaurantOwnerRepository.save(owner);

        return new RestaurantOwnerProfileResponse(
                updated.getId(),
                updated.getUserId(),
                updated.getRestaurantName(),
                updated.getCuisineType(),
                updated.getCity() != null ? updated.getCity() : updated.getAddress(),
                updated.getAddress(),
                updated.getImageUrl(),
                updated.getMinimumOrderAmount() != null ? updated.getMinimumOrderAmount() : 0.0,
                updated.getEstimatedDeliveryTime() != null ? updated.getEstimatedDeliveryTime() : 30,
                updated.getIsActive() != null ? updated.getIsActive() : true
        );
    }

    @Override
    public RestaurantOwnerEntity createRestaurantOwner(UUID userId, String restaurantName, String cuisineType, String address, MultipartFile imageFile) {
        logger.info("Creating restaurant owner profile for userId={}", userId);
        if (restaurantOwnerRepository.existsByUserId(userId)) {
            throw new IllegalArgumentException("Restaurant already registered for this user. Please update your existing restaurant profile instead.");
        }

        String imageUrl = cloudinaryImageService.uploadRestaurantImage(imageFile, userId);

        RestaurantOwnerEntity owner = new RestaurantOwnerEntity();
        owner.setUserId(userId);
        owner.setRestaurantName(restaurantName);
        owner.setCuisineType(cuisineType);
        owner.setAddress(address);
        owner.setImageUrl(imageUrl);
        owner.setIsActive(true);
        owner.setCreatedAt(LocalDateTime.now());
        owner.setUpdatedAt(LocalDateTime.now());

        return restaurantOwnerRepository.save(owner);
    }

    @Override
    public List<AdminRestaurantOwnerResponse> getAllOwnersForAdmin() {
        logger.debug("Loading all restaurant owners for admin");
        return restaurantOwnerRepository.findAll().stream()
                .sorted(Comparator.comparing(RestaurantOwnerEntity::getCreatedAt, Comparator.nullsLast(LocalDateTime::compareTo)).reversed())
                .map(owner -> new AdminRestaurantOwnerResponse(
                        owner.getId(),
                        owner.getUserId(),
                        owner.getRestaurantName(),
                        owner.getCuisineType(),
                        owner.getAddress(),
                        owner.getEmail(),
                        owner.getPhone(),
                        owner.getIsActive() == null || owner.getIsActive(),
                        owner.getCreatedAt()
                ))
                .toList();
    }
}

