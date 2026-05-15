package com.quickbite.menu.service;

import com.quickbite.menu.dto.CreateMenuItemRequest;
import com.quickbite.menu.dto.MenuItemWithAvailabilityResponse;
import org.springframework.web.multipart.MultipartFile;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public interface MenuOwnerService {

    MenuItemWithAvailabilityResponse addMenuItem(UUID ownerId, CreateMenuItemRequest request, MultipartFile imageFile);

    MenuItemWithAvailabilityResponse updateMenuItem(Long itemId, UUID ownerId, CreateMenuItemRequest request, MultipartFile imageFile);

    void deleteMenuItem(Long itemId, UUID ownerId);

    MenuItemWithAvailabilityResponse toggleAvailability(Long itemId, UUID ownerId, boolean available);

    List<MenuItemWithAvailabilityResponse> getMenuItems(Long restaurantId);

    List<MenuItemWithAvailabilityResponse> searchMenuItems(Long restaurantId, String search);

    Map<String, Object> createCategory(UUID ownerId, Map<String, String> request);

    List<Map<String, Object>> getCategories(Long restaurantId, UUID ownerId);
}

