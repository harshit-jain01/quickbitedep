package com.quickbite.restaurant.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.quickbite.restaurant.dto.RestaurantOwnerProfileResponse;
import com.quickbite.restaurant.model.RestaurantOwnerEntity;
import com.quickbite.restaurant.repository.RestaurantOwnerRepository;
import com.quickbite.restaurant.service.impl.RestaurantOwnerServiceImpl;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

@ExtendWith(MockitoExtension.class)
class RestaurantOwnerServiceTest {

    @Mock
    private RestaurantOwnerRepository restaurantOwnerRepository;

    @Mock
    private CloudinaryImageService cloudinaryImageService;

    @InjectMocks
    private RestaurantOwnerServiceImpl service;

    @Test
    void getRestaurantOwnerProfile_shouldMapDefaults() {
        UUID ownerId = UUID.randomUUID();
        RestaurantOwnerEntity owner = new RestaurantOwnerEntity();
        owner.setId(10L);
        owner.setUserId(ownerId);
        owner.setRestaurantName("R1");
        owner.setCuisineType("Indian");
        owner.setAddress("Addr");
        owner.setCity(null);
        owner.setImageUrl("img");
        owner.setMinimumOrderAmount(null);
        owner.setEstimatedDeliveryTime(null);
        owner.setIsActive(null);

        when(restaurantOwnerRepository.findByUserId(ownerId)).thenReturn(Optional.of(owner));

        var response = service.getRestaurantOwnerProfile(ownerId);

        assertEquals("Addr", response.area());
        assertEquals(0.0, response.minimumOrderAmount());
        assertEquals(30, response.estimatedDeliveryTime());
        assertEquals(true, response.isActive());
    }

    @Test
    void updateRestaurantProfile_shouldPersistUpdatedValues() {
        UUID ownerId = UUID.randomUUID();
        RestaurantOwnerEntity owner = new RestaurantOwnerEntity();
        owner.setId(11L);
        owner.setUserId(ownerId);
        owner.setRestaurantName("Old");

        RestaurantOwnerProfileResponse request = new RestaurantOwnerProfileResponse(
                11L, ownerId, "New", "Italian", "Bhopal", "Desc", "img", 299.0, 35, false
        );

        when(restaurantOwnerRepository.findByUserId(ownerId)).thenReturn(Optional.of(owner));
        when(restaurantOwnerRepository.save(any(RestaurantOwnerEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var updated = service.updateRestaurantProfile(ownerId, request);

        assertEquals("New", updated.restaurantName());
        assertEquals("Bhopal", updated.area());
        assertEquals("Desc", updated.description());
        assertEquals(false, updated.isActive());
    }

    @Test
    void createRestaurantOwner_shouldThrowWhenOwnerAlreadyExists() {
        UUID userId = UUID.randomUUID();
        when(restaurantOwnerRepository.existsByUserId(userId)).thenReturn(true);

        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> service.createRestaurantOwner(userId, "R1", "Indian", "Addr", new MockMultipartFile("f", new byte[] {1}))
        );

        assertEquals("Restaurant already registered for this user. Please update your existing restaurant profile instead.", ex.getMessage());
    }

    @Test
    void createRestaurantOwner_shouldUploadImageAndSave() {
        UUID userId = UUID.randomUUID();
        when(restaurantOwnerRepository.existsByUserId(userId)).thenReturn(false);
        when(cloudinaryImageService.uploadRestaurantImage(any(), any())).thenReturn("https://cdn/image.jpg");
        when(restaurantOwnerRepository.save(any(RestaurantOwnerEntity.class))).thenAnswer(invocation -> {
            RestaurantOwnerEntity e = invocation.getArgument(0);
            e.setId(99L);
            return e;
        });

        var created = service.createRestaurantOwner(
                userId,
                "R1",
                "Indian",
                "Addr",
                new MockMultipartFile("imageFile", "r.jpg", "image/jpeg", "img".getBytes())
        );

        assertEquals(99L, created.getId());
        assertEquals("https://cdn/image.jpg", created.getImageUrl());
    }

    @Test
    void getAllOwnersForAdmin_shouldSortByCreatedAtDescending() {
        RestaurantOwnerEntity older = new RestaurantOwnerEntity();
        older.setId(1L);
        older.setUserId(UUID.randomUUID());
        older.setRestaurantName("Old");
        older.setCreatedAt(LocalDateTime.now().minusDays(1));
        older.setIsActive(true);

        RestaurantOwnerEntity newer = new RestaurantOwnerEntity();
        newer.setId(2L);
        newer.setUserId(UUID.randomUUID());
        newer.setRestaurantName("New");
        newer.setCreatedAt(LocalDateTime.now());
        newer.setIsActive(true);

        when(restaurantOwnerRepository.findAll()).thenReturn(List.of(older, newer));

        var response = service.getAllOwnersForAdmin();

        assertEquals(2L, response.get(0).id());
        assertEquals(1L, response.get(1).id());
        verify(restaurantOwnerRepository).findAll();
    }
}
