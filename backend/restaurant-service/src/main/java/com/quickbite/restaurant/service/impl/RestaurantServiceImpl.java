package com.quickbite.restaurant.service.impl;

import com.quickbite.restaurant.dto.CategoryResponse;
import com.quickbite.restaurant.dto.RestaurantResponse;
import com.quickbite.restaurant.model.Category;
import com.quickbite.restaurant.model.Restaurant;
import com.quickbite.restaurant.model.RestaurantOwnerEntity;
import com.quickbite.restaurant.repository.RestaurantOwnerRepository;
import com.quickbite.restaurant.repository.RestaurantRepository;
import com.quickbite.restaurant.service.RestaurantService;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class RestaurantServiceImpl implements RestaurantService {

    private static final Logger logger = LoggerFactory.getLogger(RestaurantServiceImpl.class);

    private static final long OWNER_RESTAURANT_ID_OFFSET = 1_000_000L;
    private static final String DEFAULT_OWNER_IMAGE = "https://images.unsplash.com/photo-1517248135467-4c7edcad34c4?auto=format&fit=crop&w=900&q=80";

    private final RestaurantRepository restaurantRepository;
    private final RestaurantOwnerRepository restaurantOwnerRepository;

    public RestaurantServiceImpl(
            RestaurantRepository restaurantRepository,
            RestaurantOwnerRepository restaurantOwnerRepository
    ) {
        this.restaurantRepository = restaurantRepository;
        this.restaurantOwnerRepository = restaurantOwnerRepository;
    }

    @Override
    public List<CategoryResponse> getCategories() {
        logger.debug("Loading restaurant categories");
        return restaurantRepository.findAllCategories().stream()
                .map(this::toCategoryResponse)
                .toList();
    }

    @Override
    public List<RestaurantResponse> getRestaurants(String category, String search) {
        logger.debug("Loading restaurants with category={} search={}", category, search);
        String normalizedCategory = normalize(category);
        String normalizedSearch = normalize(search);

        List<Restaurant> catalog = new ArrayList<>(restaurantRepository.findAll(null, null));
        catalog.addAll(restaurantOwnerRepository.findAll().stream()
                .filter(owner -> owner.getIsActive() == null || owner.getIsActive())
                .map(this::toRestaurantFromOwner)
                .toList());

        return catalog.stream()
                .filter(restaurant -> normalizedCategory.isBlank()
                        || restaurant.category().toLowerCase(Locale.ROOT).equals(normalizedCategory))
                .filter(restaurant -> normalizedSearch.isBlank()
                        || restaurant.name().toLowerCase(Locale.ROOT).contains(normalizedSearch)
                        || restaurant.cuisines().stream()
                        .map(value -> value.toLowerCase(Locale.ROOT))
                        .anyMatch(value -> value.contains(normalizedSearch)))
                .sorted(Comparator.comparingDouble(Restaurant::rating).reversed())
                .map(this::toRestaurantResponse)
                .toList();
    }

    @Override
    public RestaurantResponse getRestaurant(Long restaurantId) {
        logger.info("Loading restaurant details for restaurantId={}", restaurantId);
        if (restaurantId >= OWNER_RESTAURANT_ID_OFFSET) {
            long ownerId = restaurantId - OWNER_RESTAURANT_ID_OFFSET;
            RestaurantOwnerEntity owner = restaurantOwnerRepository.findById(ownerId)
                    .orElseThrow(() -> new IllegalArgumentException("Restaurant not found"));
            if (owner.getIsActive() != null && !owner.getIsActive()) {
                throw new IllegalArgumentException("Restaurant not found");
            }
            return toRestaurantResponse(toRestaurantFromOwner(owner));
        }
        return toRestaurantResponse(restaurantRepository.findById(restaurantId));
    }

    @Override
    public List<RestaurantResponse> getAllRestaurantsForAdmin() {
        return getRestaurants(null, null);
    }

    @Override
    public void deleteRestaurantById(Long restaurantId) {
        logger.warn("Deleting restaurant by id={}", restaurantId);
        if (restaurantId >= OWNER_RESTAURANT_ID_OFFSET) {
            long ownerId = restaurantId - OWNER_RESTAURANT_ID_OFFSET;
            if (!restaurantOwnerRepository.existsById(ownerId)) {
                throw new IllegalArgumentException("Restaurant not found");
            }
            restaurantOwnerRepository.deleteById(ownerId);
            return;
        }

        if (!restaurantRepository.deleteById(restaurantId)) {
            throw new IllegalArgumentException("Restaurant not found");
        }
    }

    private Restaurant toRestaurantFromOwner(RestaurantOwnerEntity owner) {
        String cuisineType = owner.getCuisineType() == null || owner.getCuisineType().isBlank()
                ? "Multi-cuisine"
                : owner.getCuisineType().trim();
        List<String> cuisines = parseCuisines(cuisineType);
        String area = owner.getCity() != null && !owner.getCity().isBlank()
                ? owner.getCity()
                : (owner.getAddress() != null && !owner.getAddress().isBlank() ? owner.getAddress() : "Your area");
        int deliveryTime = owner.getEstimatedDeliveryTime() != null ? owner.getEstimatedDeliveryTime() : 30;
        int priceForTwo = owner.getMinimumOrderAmount() != null
                ? Math.max(200, (int) Math.round(owner.getMinimumOrderAmount() * 2))
                : 400;
        String imageUrl = owner.getImageUrl() != null && !owner.getImageUrl().isBlank()
                ? owner.getImageUrl()
                : DEFAULT_OWNER_IMAGE;

        return new Restaurant(
                OWNER_RESTAURANT_ID_OFFSET + owner.getId(),
                owner.getRestaurantName(),
                cuisines.get(0),
                area,
                cuisines,
                4.2,
                deliveryTime,
                priceForTwo,
                2.5,
                imageUrl,
                "Freshly onboarded restaurant partner"
        );
    }

    private List<String> parseCuisines(String cuisineType) {
        List<String> cuisines = java.util.Arrays.stream(cuisineType.split(","))
                .map(String::trim)
                .filter(value -> !value.isBlank())
                .toList();
        if (cuisines.isEmpty()) {
            return List.of("Multi-cuisine");
        }
        return cuisines;
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }

    private CategoryResponse toCategoryResponse(Category category) {
        return new CategoryResponse(category.id(), category.name(), category.imageUrl());
    }

    private RestaurantResponse toRestaurantResponse(Restaurant restaurant) {
        return new RestaurantResponse(
                restaurant.id(),
                restaurant.name(),
                restaurant.category(),
                restaurant.area(),
                restaurant.cuisines(),
                restaurant.rating(),
                restaurant.deliveryTimeMinutes(),
                restaurant.priceForTwo(),
                restaurant.distanceKm(),
                restaurant.imageUrl(),
                restaurant.description()
        );
    }
}

