package com.quickbite.restaurant.controller;

import com.quickbite.restaurant.dto.AdminRestaurantOwnerResponse;
import com.quickbite.restaurant.service.RestaurantOwnerService;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/owners")
public class AdminOwnerController {

    private static final Logger logger = LoggerFactory.getLogger(AdminOwnerController.class);

    private final RestaurantOwnerService restaurantOwnerService;

    public AdminOwnerController(RestaurantOwnerService restaurantOwnerService) {
        this.restaurantOwnerService = restaurantOwnerService;
    }

    @GetMapping
    public List<AdminRestaurantOwnerResponse> getOwners(@RequestHeader("X-Authenticated-Role") String role) {
        if (!"ADMIN".equalsIgnoreCase(role)) {
            throw new AccessDeniedException("Only admin can access this endpoint");
        }
        logger.info("Admin requested owners list via /admin/owners");
        return restaurantOwnerService.getAllOwnersForAdmin();
    }
}

