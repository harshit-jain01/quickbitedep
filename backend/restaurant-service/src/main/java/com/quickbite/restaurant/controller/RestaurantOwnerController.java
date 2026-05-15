package com.quickbite.restaurant.controller;

import com.quickbite.restaurant.client.OrderServiceClient;
import com.quickbite.restaurant.client.ReviewServiceClient;
import com.quickbite.restaurant.dto.RestaurantOwnerProfileResponse;
import com.quickbite.restaurant.security.JwtService;
import com.quickbite.restaurant.service.RestaurantOwnerService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.MediaType;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import java.util.UUID;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@RestController
@RequestMapping("/api/v1/restaurant-owner")
public class RestaurantOwnerController {

    private static final Logger logger = LoggerFactory.getLogger(RestaurantOwnerController.class);

    private final RestaurantOwnerService restaurantOwnerService;
    private final JwtService jwtService;
    private final OrderServiceClient orderServiceClient;
    private final ReviewServiceClient reviewServiceClient;

    public RestaurantOwnerController(
            RestaurantOwnerService restaurantOwnerService,
            JwtService jwtService,
            OrderServiceClient orderServiceClient,
            ReviewServiceClient reviewServiceClient
    ) {
        this.restaurantOwnerService = restaurantOwnerService;
        this.jwtService = jwtService;
        this.orderServiceClient = orderServiceClient;
        this.reviewServiceClient = reviewServiceClient;
    }

    private UUID extractUserIdFromToken(HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);
            String userId = jwtService.extractUserId(token);
            if (userId != null) {
                return UUID.fromString(userId);
            }
        }
        return null;
    }

    @PostMapping(value = "/register", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public RestaurantOwnerProfileResponse register(
            HttpServletRequest request,
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam String restaurantName,
            @RequestParam String cuisineType,
            @RequestParam String address,
            @RequestParam("imageFile") MultipartFile imageFile
    ) {
        logger.info("Restaurant owner registration request received");
        if (userDetails == null) {
            throw new IllegalArgumentException("User not authenticated");
        }

        if (restaurantName == null || restaurantName.trim().isEmpty()) {
            throw new IllegalArgumentException("Restaurant name is required");
        }
        if (cuisineType == null || cuisineType.trim().isEmpty()) {
            throw new IllegalArgumentException("Cuisine type is required");
        }
        if (address == null || address.trim().isEmpty()) {
            throw new IllegalArgumentException("Address is required");
        }
        if (imageFile == null || imageFile.isEmpty()) {
            throw new IllegalArgumentException("Restaurant image is required");
        }

        UUID userId = extractUserIdFromToken(request);
        if (userId == null) {
            throw new IllegalArgumentException("Unable to extract user ID from token. Make sure the JWT token contains the userId claim.");
        }

        restaurantOwnerService.createRestaurantOwner(userId, restaurantName.trim(), cuisineType.trim(), address.trim(), imageFile);
        return restaurantOwnerService.getRestaurantOwnerProfile(userId);
    }

    @GetMapping("/profile")
    public RestaurantOwnerProfileResponse getProfile(HttpServletRequest request) {
        logger.debug("Restaurant owner profile requested");
        UUID ownerId = extractUserIdFromToken(request);
        if (ownerId == null) {
            throw new IllegalArgumentException("Unable to extract user ID from token");
        }
        return restaurantOwnerService.getRestaurantOwnerProfile(ownerId);
    }

    @PutMapping("/profile")
    public RestaurantOwnerProfileResponse updateProfile(
            HttpServletRequest request,
            @RequestBody RestaurantOwnerProfileResponse body
    ) {
        logger.info("Restaurant owner profile update request received");
        UUID ownerId = extractUserIdFromToken(request);
        if (ownerId == null) {
            throw new IllegalArgumentException("Unable to extract user ID from token");
        }
        return restaurantOwnerService.updateRestaurantProfile(ownerId, body);
    }

    @GetMapping("/orders")
    public List<Map<String, Object>> getOrders(
            HttpServletRequest request,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        logger.info("Restaurant owner orders requested");
        UUID ownerId = extractUserIdFromToken(request);
        if (ownerId == null) {
            throw new IllegalArgumentException("Unable to extract user ID from token");
        }

        String ownerEmail = userDetails != null ? userDetails.getUsername() : "restaurant-owner@quickbite.local";
        String ownerRestaurantName = restaurantOwnerService.getRestaurantOwnerProfile(ownerId).restaurantName();

        List<Map<String, Object>> allOrders = orderServiceClient.getOrders(ownerEmail, "DELIVERY_AGENT");

        if (allOrders == null) {
            return List.of();
        }

        return allOrders.stream()
                .filter(order -> {
                    Object restaurantName = order.get("restaurantName");
                    return restaurantName != null && ownerRestaurantName.equalsIgnoreCase(String.valueOf(restaurantName));
                })
                .toList();
    }

    @PatchMapping("/orders/{orderReference}")
    public Map<String, Object> updateOrderStatus(
            HttpServletRequest request,
            @PathVariable String orderReference,
            @RequestBody Map<String, String> body
    ) {
        logger.info("Restaurant owner order status update requested for orderReference={}", orderReference);
        UUID ownerId = extractUserIdFromToken(request);
        if (ownerId == null) {
            throw new IllegalArgumentException("Unable to extract user ID from token");
        }

        String ownerRestaurantName = restaurantOwnerService.getRestaurantOwnerProfile(ownerId).restaurantName();
        if (ownerRestaurantName == null || ownerRestaurantName.isBlank()) {
            throw new IllegalArgumentException("Restaurant profile not found");
        }

        String deliveryStatus = body == null ? "" : body.getOrDefault("deliveryStatus", "").trim().toUpperCase(Locale.ROOT);
        if (deliveryStatus.isBlank()) {
            throw new IllegalArgumentException("deliveryStatus is required");
        }

        return orderServiceClient.updateOrderStatus("SYSTEM", orderReference, Map.of("deliveryStatus", deliveryStatus));
    }

    @GetMapping("/analytics")
    public java.util.Map<String, Object> getAnalytics(@AuthenticationPrincipal UserDetails userDetails) {
        // Mock endpoint - returns analytics data
        return java.util.Map.of(
                "period", "daily",
                "revenue", 0,
                "orders", 0,
                "avgOrderValue", 0,
                "topItems", java.util.List.of()
        );
    }

    @GetMapping("/reviews")
    public List<Map<String, Object>> getReviews(HttpServletRequest request) {
        UUID ownerId = extractUserIdFromToken(request);
        if (ownerId == null) {
            throw new IllegalArgumentException("Unable to extract user ID from token");
        }
        RestaurantOwnerProfileResponse profile = restaurantOwnerService.getRestaurantOwnerProfile(ownerId);
        if (profile == null || profile.restaurantId() == null) {
            return List.of();
        }
        return reviewServiceClient.getRestaurantReviews(profile.restaurantId());
    }
}
