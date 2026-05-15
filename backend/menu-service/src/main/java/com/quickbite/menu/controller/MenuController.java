package com.quickbite.menu.controller;

import com.quickbite.menu.dto.MenuGroupResponse;
import com.quickbite.menu.service.MenuService;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/menu")
public class MenuController {

    private static final Logger logger = LoggerFactory.getLogger(MenuController.class);

    private final MenuService menuService;

    public MenuController(MenuService menuService) {
        this.menuService = menuService;
    }

    @GetMapping("/restaurants/{restaurantId}")
    public List<MenuGroupResponse> getRestaurantMenu(@PathVariable Long restaurantId) {
        logger.debug("Menu request for restaurantId={}", restaurantId);
        return menuService.getRestaurantMenu(restaurantId);
    }
}
