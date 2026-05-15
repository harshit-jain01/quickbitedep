package com.quickbite.menu.service.impl;

import com.quickbite.menu.dto.CreateMenuItemRequest;
import com.quickbite.menu.dto.MenuItemWithAvailabilityResponse;
import com.quickbite.menu.model.MenuItemEntity;
import com.quickbite.menu.repository.MenuItemRepository;
import com.quickbite.menu.service.CloudinaryImageService;
import com.quickbite.menu.service.MenuOwnerService;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class MenuOwnerServiceImpl implements MenuOwnerService {

    private static final Logger logger = LoggerFactory.getLogger(MenuOwnerServiceImpl.class);

    private final MenuItemRepository menuItemRepository;
    private final CloudinaryImageService cloudinaryImageService;

    public MenuOwnerServiceImpl(MenuItemRepository menuItemRepository, CloudinaryImageService cloudinaryImageService) {
        this.menuItemRepository = menuItemRepository;
        this.cloudinaryImageService = cloudinaryImageService;
    }

    @Override
    public MenuItemWithAvailabilityResponse addMenuItem(UUID ownerId, CreateMenuItemRequest request, MultipartFile imageFile) {
        logger.info("Adding menu item for owner={} restaurantId={} itemName={}", ownerId, request.restaurantId(), request.itemName());
        try {
            if (request.itemName() == null || request.itemName().trim().isEmpty()) {
                throw new IllegalArgumentException("Item name is required");
            }
            if (request.price() == null || request.price() <= 0) {
                throw new IllegalArgumentException("Price must be greater than 0");
            }
            if (request.restaurantId() == null) {
                throw new IllegalArgumentException("Restaurant ID is required");
            }
            if (imageFile == null || imageFile.isEmpty()) {
                throw new IllegalArgumentException("Menu item image is required");
            }

            String imageUrl = cloudinaryImageService.uploadMenuItemImage(imageFile, request.restaurantId());

            MenuItemEntity item = new MenuItemEntity();
            item.setRestaurantId(request.restaurantId());
            item.setCategoryId(request.categoryId() != null ? request.categoryId() : 1L);
            item.setName(request.itemName().trim());
            item.setDescription(request.description() != null ? request.description() : "");
            item.setPrice(request.price());
            item.setImageUrl(imageUrl);
            item.setIsVegetarian(request.vegetarian() != null ? request.vegetarian() : false);
            item.setIsBestseller(request.bestseller() != null ? request.bestseller() : false);
            item.setIsAvailable(request.isAvailable() != null ? request.isAvailable() : true);
            item.setCreatedAt(LocalDateTime.now());
            item.setUpdatedAt(LocalDateTime.now());

            MenuItemEntity saved = menuItemRepository.save(item);
            return convertToResponse(saved);
        } catch (Exception e) {
            if (e instanceof IllegalArgumentException || e instanceof IllegalStateException) {
                throw (RuntimeException) e;
            }
            logger.error("Error adding menu item for owner={}", ownerId, e);
            throw new RuntimeException("Failed to add menu item: " + e.getMessage(), e);
        }
    }

    @Override
    public MenuItemWithAvailabilityResponse updateMenuItem(Long itemId, UUID ownerId, CreateMenuItemRequest request, MultipartFile imageFile) {
        logger.info("Updating menu item for owner={} itemId={}", ownerId, itemId);
        try {
            if (itemId == null || itemId <= 0) {
                throw new IllegalArgumentException("Invalid item ID");
            }
            if (request.itemName() == null || request.itemName().trim().isEmpty()) {
                throw new IllegalArgumentException("Item name is required");
            }
            if (request.price() == null || request.price() <= 0) {
                throw new IllegalArgumentException("Price must be greater than 0");
            }

            MenuItemEntity existing = menuItemRepository.findById(itemId)
                    .orElseThrow(() -> new IllegalArgumentException("Menu item not found"));

            if (request.categoryId() != null) {
                existing.setCategoryId(request.categoryId());
            }
            existing.setName(request.itemName().trim());
            existing.setDescription(request.description() != null ? request.description() : "");
            existing.setPrice(request.price());
            if (imageFile != null && !imageFile.isEmpty()) {
                String imageUrl = cloudinaryImageService.uploadMenuItemImage(imageFile, existing.getRestaurantId());
                existing.setImageUrl(imageUrl);
            }
            existing.setIsVegetarian(request.vegetarian() != null ? request.vegetarian() : existing.getIsVegetarian());
            existing.setIsBestseller(request.bestseller() != null ? request.bestseller() : existing.getIsBestseller());
            if (request.isAvailable() != null) {
                existing.setIsAvailable(request.isAvailable());
            }
            existing.setUpdatedAt(LocalDateTime.now());

            MenuItemEntity updated = menuItemRepository.save(existing);
            return convertToResponse(updated);
        } catch (Exception e) {
            if (e instanceof IllegalArgumentException || e instanceof IllegalStateException) {
                throw (RuntimeException) e;
            }
            logger.error("Error updating menu item owner={} itemId={}", ownerId, itemId, e);
            throw new RuntimeException("Failed to update menu item: " + e.getMessage(), e);
        }
    }

    @Override
    public void deleteMenuItem(Long itemId, UUID ownerId) {
        logger.warn("Deleting menu item for owner={} itemId={}", ownerId, itemId);
        if (menuItemRepository.existsById(itemId)) {
            menuItemRepository.deleteById(itemId);
        } else {
            throw new IllegalArgumentException("Menu item not found");
        }
    }

    @Override
    public MenuItemWithAvailabilityResponse toggleAvailability(Long itemId, UUID ownerId, boolean available) {
        logger.info("Toggling availability owner={} itemId={} available={}", ownerId, itemId, available);
        MenuItemEntity existing = menuItemRepository.findById(itemId)
                .orElseThrow(() -> new IllegalArgumentException("Menu item not found"));

        existing.setIsAvailable(available);
        existing.setUpdatedAt(LocalDateTime.now());

        MenuItemEntity updated = menuItemRepository.save(existing);
        return convertToResponse(updated);
    }

    @Override
    public List<MenuItemWithAvailabilityResponse> getMenuItems(Long restaurantId) {
        return menuItemRepository.findByRestaurantId(restaurantId).stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }

    @Override
    public List<MenuItemWithAvailabilityResponse> searchMenuItems(Long restaurantId, String search) {
        return menuItemRepository.searchByName(restaurantId, search).stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }

    @Override
    public Map<String, Object> createCategory(UUID ownerId, Map<String, String> request) {
        Map<String, Object> response = new HashMap<>();
        response.put("categoryId", UUID.randomUUID());
        response.put("categoryName", request.getOrDefault("categoryName", "New Category"));
        response.put("description", request.getOrDefault("description", ""));
        response.put("ownerId", ownerId);
        return response;
    }

    @Override
    public List<Map<String, Object>> getCategories(Long restaurantId, UUID ownerId) {
        List<Map<String, Object>> categories = new ArrayList<>();

        String[] categoryNames = {"Starters", "Main Course", "Desserts", "Beverages", "Recommended"};
        long categoryId = 1L;
        for (String categoryName : categoryNames) {
            Map<String, Object> category = new HashMap<>();
            category.put("categoryId", categoryId);
            category.put("categoryName", categoryName);
            category.put("restaurantId", restaurantId);

            List<MenuItemEntity> items = menuItemRepository.findByRestaurantIdAndCategoryId(restaurantId, categoryId);
            List<Map<String, Object>> itemsList = new ArrayList<>();
            for (MenuItemEntity item : items) {
                Map<String, Object> itemMap = new HashMap<>();
                itemMap.put("itemId", item.getId());
                itemMap.put("itemName", item.getName());
                itemMap.put("description", item.getDescription());
                itemMap.put("price", item.getPrice());
                itemMap.put("imageUrl", item.getImageUrl());
                itemMap.put("isAvailable", item.getIsAvailable());
                itemsList.add(itemMap);
            }
            category.put("items", itemsList);

            categories.add(category);
            categoryId++;
        }
        return categories;
    }

    private MenuItemWithAvailabilityResponse convertToResponse(MenuItemEntity item) {
        long createdTime = item.getCreatedAt() != null ? item.getCreatedAt().toString().hashCode() : 0;
        long updatedTime = item.getUpdatedAt() != null ? item.getUpdatedAt().toString().hashCode() : 0;

        return new MenuItemWithAvailabilityResponse(
                item.getId(),
                item.getRestaurantId(),
                item.getName(),
                item.getDescription(),
                "Category",
                item.getPrice(),
                item.getIsVegetarian() != null ? item.getIsVegetarian() : false,
                item.getIsBestseller() != null ? item.getIsBestseller() : false,
                item.getImageUrl(),
                item.getIsAvailable() != null ? item.getIsAvailable() : true,
                createdTime,
                updatedTime
        );
    }
}

