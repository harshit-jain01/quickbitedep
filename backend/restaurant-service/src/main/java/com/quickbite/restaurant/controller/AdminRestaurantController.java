package com.quickbite.restaurant.controller;

import com.quickbite.restaurant.dto.AdminRestaurantOwnerResponse;
import com.quickbite.restaurant.dto.RestaurantResponse;
import com.quickbite.restaurant.service.RestaurantOwnerService;
import com.quickbite.restaurant.service.RestaurantService;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/restaurants")
public class AdminRestaurantController {

    private static final Logger logger = LoggerFactory.getLogger(AdminRestaurantController.class);

    private final RestaurantService restaurantService;
    private final RestaurantOwnerService restaurantOwnerService;

    public AdminRestaurantController(RestaurantService restaurantService, RestaurantOwnerService restaurantOwnerService) {
        this.restaurantService = restaurantService;
        this.restaurantOwnerService = restaurantOwnerService;
    }

    @GetMapping
    public List<RestaurantResponse> getRestaurants(@RequestHeader("X-Authenticated-Role") String role) {
        verifyAdmin(role);
        logger.info("Admin requested restaurant list");
        return restaurantService.getAllRestaurantsForAdmin();
    }

    @GetMapping("/owners")
    public List<AdminRestaurantOwnerResponse> getOwners(@RequestHeader("X-Authenticated-Role") String role) {
        verifyAdmin(role);
        logger.info("Admin requested restaurant owner list");
        return restaurantOwnerService.getAllOwnersForAdmin();
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteRestaurant(
            @RequestHeader("X-Authenticated-Role") String role,
            @PathVariable("id") Long restaurantId
    ) {
        verifyAdmin(role);
        logger.warn("Admin deleting restaurantId={}", restaurantId);
        restaurantService.deleteRestaurantById(restaurantId);
    }

    private void verifyAdmin(String role) {
        if (!"ADMIN".equalsIgnoreCase(role)) {
            throw new org.springframework.security.access.AccessDeniedException("Only admin can access this endpoint");
        }
    }
}

