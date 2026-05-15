package com.quickbite.restaurant.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.quickbite.restaurant.model.Category;
import com.quickbite.restaurant.model.Restaurant;
import com.quickbite.restaurant.model.RestaurantOwnerEntity;
import com.quickbite.restaurant.repository.RestaurantOwnerRepository;
import com.quickbite.restaurant.repository.RestaurantRepository;
import com.quickbite.restaurant.service.impl.RestaurantServiceImpl;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class RestaurantServiceTest {

    @Mock
    private RestaurantRepository restaurantRepository;

    @Mock
    private RestaurantOwnerRepository restaurantOwnerRepository;

    @InjectMocks
    private RestaurantServiceImpl restaurantService;

    @Test
    void getCategories_shouldMapRepositoryResult() {
        when(restaurantRepository.findAllCategories()).thenReturn(List.of(
                new Category("pizza", "Pizza", "img")
        ));

        var result = restaurantService.getCategories();

        assertEquals(1, result.size());
        assertEquals("Pizza", result.get(0).name());
    }

    @Test
    void getRestaurants_shouldIncludeOwnerRestaurantAndFilterBySearch() {
        when(restaurantRepository.findAll(null, null)).thenReturn(List.of(
                new Restaurant(1L, "Spice Route", "North Indian", "MP Nagar", List.of("North Indian"), 4.5, 30, 400, 2.1, "img", "desc")
        ));

        RestaurantOwnerEntity owner = new RestaurantOwnerEntity();
        owner.setId(10L);
        owner.setUserId(UUID.randomUUID());
        owner.setRestaurantName("Owner Pizza");
        owner.setCuisineType("Pizza,Italian");
        owner.setCity("Bhopal");
        owner.setIsActive(true);
        owner.setMinimumOrderAmount(250.0);
        owner.setEstimatedDeliveryTime(25);
        when(restaurantOwnerRepository.findAll()).thenReturn(List.of(owner));

        var result = restaurantService.getRestaurants(null, "pizza");

        assertEquals(1, result.size());
        assertEquals("Owner Pizza", result.get(0).name());
    }

    @Test
    void getRestaurant_shouldThrow_whenOwnerRestaurantInactive() {
        RestaurantOwnerEntity owner = new RestaurantOwnerEntity();
        owner.setId(5L);
        owner.setRestaurantName("Inactive");
        owner.setIsActive(false);

        when(restaurantOwnerRepository.findById(5L)).thenReturn(Optional.of(owner));

        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> restaurantService.getRestaurant(1_000_005L)
        );

        assertEquals("Restaurant not found", ex.getMessage());
    }

    @Test
    void deleteRestaurantById_shouldDeleteOwnerRestaurant_whenExists() {
        when(restaurantOwnerRepository.existsById(12L)).thenReturn(true);

        restaurantService.deleteRestaurantById(1_000_012L);

        verify(restaurantOwnerRepository).deleteById(12L);
    }

    @Test
    void deleteRestaurantById_shouldThrow_whenCatalogRestaurantMissing() {
        when(restaurantRepository.deleteById(99L)).thenReturn(false);

        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> restaurantService.deleteRestaurantById(99L)
        );

        assertEquals("Restaurant not found", ex.getMessage());
    }
}

