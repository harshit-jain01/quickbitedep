package com.quickbite.menu.service.impl;

import com.quickbite.menu.dto.MenuGroupResponse;
import com.quickbite.menu.dto.MenuItemResponse;
import com.quickbite.menu.model.MenuItem;
import com.quickbite.menu.model.MenuItemEntity;
import com.quickbite.menu.repository.MenuItemRepository;
import com.quickbite.menu.repository.MenuRepository;
import com.quickbite.menu.service.MenuService;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class MenuServiceImpl implements MenuService {

    private static final Logger logger = LoggerFactory.getLogger(MenuServiceImpl.class);

    private static final long OWNER_RESTAURANT_ID_OFFSET = 1_000_000L;

    private final MenuRepository menuRepository;
    private final MenuItemRepository menuItemRepository;

    public MenuServiceImpl(MenuRepository menuRepository, MenuItemRepository menuItemRepository) {
        this.menuRepository = menuRepository;
        this.menuItemRepository = menuItemRepository;
    }

    @Override
    public List<MenuGroupResponse> getRestaurantMenu(Long restaurantId) {
        logger.debug("Fetching menu for restaurantId={}", restaurantId);
        if (restaurantId == null || restaurantId <= 0) {
            logger.warn("Invalid restaurantId supplied for menu lookup: {}", restaurantId);
            return List.of();
        }

        List<MenuItemResponse> items = getDynamicMenuItems(restaurantId);
        if (items.isEmpty()) {
            items = menuRepository.findByRestaurantId(restaurantId).stream()
                    .map(this::toResponse)
                    .toList();
        }

        Map<String, List<MenuItemResponse>> grouped = items.stream()
                .collect(Collectors.groupingBy(MenuItemResponse::category));

        return grouped.entrySet().stream()
                .map(entry -> new MenuGroupResponse(entry.getKey(), entry.getValue()))
                .toList();
    }

    private List<MenuItemResponse> getDynamicMenuItems(Long restaurantId) {
        Set<Long> candidateRestaurantIds = new LinkedHashSet<>();
        candidateRestaurantIds.add(restaurantId);
        if (restaurantId >= OWNER_RESTAURANT_ID_OFFSET) {
            candidateRestaurantIds.add(restaurantId - OWNER_RESTAURANT_ID_OFFSET);
        } else {
            candidateRestaurantIds.add(restaurantId + OWNER_RESTAURANT_ID_OFFSET);
        }

        List<MenuItemResponse> responses = new ArrayList<>();
        for (Long candidateId : candidateRestaurantIds) {
            try {
                responses.addAll(menuItemRepository.findByRestaurantIdAndIsAvailableTrue(candidateId).stream()
                        .map(this::toResponse)
                        .toList());
            } catch (RuntimeException ex) {
                logger.warn("Menu lookup failed for restaurantId={}", candidateId, ex);
            }
        }
        return responses;
    }

    private MenuItemResponse toResponse(MenuItem menuItem) {
        return new MenuItemResponse(
                menuItem.id(),
                menuItem.restaurantId(),
                menuItem.name(),
                menuItem.description(),
                menuItem.category(),
                menuItem.price(),
                menuItem.vegetarian(),
                menuItem.bestseller(),
                menuItem.imageUrl()
        );
    }

    private MenuItemResponse toResponse(MenuItemEntity menuItem) {
        return new MenuItemResponse(
                menuItem.getId(),
                menuItem.getRestaurantId(),
                menuItem.getName(),
                menuItem.getDescription(),
                categoryName(menuItem.getCategoryId()),
                menuItem.getPrice() != null ? menuItem.getPrice() : 0.0,
                menuItem.getIsVegetarian() != null && menuItem.getIsVegetarian(),
                menuItem.getIsBestseller() != null && menuItem.getIsBestseller(),
                menuItem.getImageUrl()
        );
    }

    private String categoryName(Long categoryId) {
        if (categoryId == null) {
            return "Recommended";
        }
        return switch (categoryId.intValue()) {
            case 1 -> "Starters";
            case 2 -> "Main Course";
            case 3 -> "Desserts";
            case 4 -> "Beverages";
            case 5 -> "Recommended";
            default -> "Category " + categoryId;
        };
    }
}

