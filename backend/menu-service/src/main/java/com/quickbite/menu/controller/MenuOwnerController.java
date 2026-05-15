package com.quickbite.menu.controller;

import com.quickbite.menu.dto.CreateMenuItemRequest;
import com.quickbite.menu.dto.MenuItemWithAvailabilityResponse;
import com.quickbite.menu.service.MenuOwnerService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@RestController
@RequestMapping("/api/v1/menu")
public class MenuOwnerController {

    private static final Logger logger = LoggerFactory.getLogger(MenuOwnerController.class);

    private final MenuOwnerService menuOwnerService;

    public MenuOwnerController(MenuOwnerService menuOwnerService) {
        this.menuOwnerService = menuOwnerService;
    }

    @PostMapping("/categories")
    @ResponseStatus(HttpStatus.CREATED)
    public Map<String, Object> createCategory(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestBody Map<String, String> request
    ) {
        UUID ownerId = userDetails != null ? UUID.fromString(userDetails.getUsername()) : UUID.randomUUID();
        logger.info("Create category request for owner={}", ownerId);
        return menuOwnerService.createCategory(ownerId, request);
    }

    @GetMapping("/restaurants/{restaurantId}/categories")
    public List<Map<String, Object>> getCategories(
            @PathVariable Long restaurantId,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        logger.debug("Get categories request for restaurantId={}", restaurantId);
        try {
            // Allow both authenticated and unauthenticated requests
            // The ownerId is optional for GET categories
            UUID ownerId = userDetails != null ? UUID.fromString(userDetails.getUsername()) : UUID.randomUUID();

            // Validate restaurantId
            if (restaurantId == null || restaurantId <= 0) {
                throw new IllegalArgumentException("Invalid restaurant ID");
            }

            return menuOwnerService.getCategories(restaurantId, ownerId);
        } catch (Exception e) {
            throw new IllegalArgumentException("Failed to get categories: " + e.getMessage(), e);
        }
    }

    @PostMapping("/items")
    @ResponseStatus(HttpStatus.CREATED)
    public MenuItemWithAvailabilityResponse addMenuItem(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam Long restaurantId,
            @RequestParam(required = false) Long categoryId,
            @RequestParam String itemName,
            @RequestParam(required = false) String description,
            @RequestParam Double price,
            @RequestParam(name = "isAvailable", required = false) Boolean isAvailable,
            @RequestParam(required = false) Boolean vegetarian,
            @RequestParam(required = false) Boolean bestseller,
            @RequestParam("imageFile") MultipartFile imageFile
    ) {
        UUID ownerId = userDetails != null ? UUID.fromString(userDetails.getUsername()) : UUID.randomUUID();
        logger.info("Add menu item request for owner={} restaurantId={} itemName={}", ownerId, restaurantId, itemName);
        CreateMenuItemRequest request = new CreateMenuItemRequest(
                restaurantId,
                categoryId,
                itemName,
                description,
                price,
                null,
                isAvailable,
                vegetarian,
                bestseller
        );
        return menuOwnerService.addMenuItem(ownerId, request, imageFile);
    }

    @PutMapping("/items/{itemId}")
    public MenuItemWithAvailabilityResponse updateMenuItem(
            @PathVariable Long itemId,
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam Long restaurantId,
            @RequestParam(required = false) Long categoryId,
            @RequestParam String itemName,
            @RequestParam(required = false) String description,
            @RequestParam Double price,
            @RequestParam(name = "isAvailable", required = false) Boolean isAvailable,
            @RequestParam(required = false) Boolean vegetarian,
            @RequestParam(required = false) Boolean bestseller,
            @RequestParam(name = "imageFile", required = false) MultipartFile imageFile
    ) {
        UUID ownerId = userDetails != null ? UUID.fromString(userDetails.getUsername()) : UUID.randomUUID();
        logger.info("Update menu item request for owner={} itemId={}", ownerId, itemId);
        CreateMenuItemRequest request = new CreateMenuItemRequest(
                restaurantId,
                categoryId,
                itemName,
                description,
                price,
                null,
                isAvailable,
                vegetarian,
                bestseller
        );
        return menuOwnerService.updateMenuItem(itemId, ownerId, request, imageFile);
    }

    @DeleteMapping("/items/{itemId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteMenuItem(
            @PathVariable Long itemId,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        UUID ownerId = userDetails != null ? UUID.fromString(userDetails.getUsername()) : UUID.randomUUID();
        logger.warn("Delete menu item request for owner={} itemId={}", ownerId, itemId);
        menuOwnerService.deleteMenuItem(itemId, ownerId);
    }

    @PatchMapping("/items/{itemId}/availability")
    public MenuItemWithAvailabilityResponse toggleAvailability(
            @PathVariable Long itemId,
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestBody Map<String, Boolean> request
    ) {
        UUID ownerId = userDetails != null ? UUID.fromString(userDetails.getUsername()) : UUID.randomUUID();
        logger.info("Toggle menu item availability request for owner={} itemId={} available={}", ownerId, itemId, request.getOrDefault("isAvailable", request.getOrDefault("available", true)));
        boolean available = request.getOrDefault("isAvailable", request.getOrDefault("available", true));
        return menuOwnerService.toggleAvailability(itemId, ownerId, available);
    }
}

