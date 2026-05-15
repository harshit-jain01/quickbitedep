package com.quickbite.menu.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.quickbite.menu.dto.CreateMenuItemRequest;
import com.quickbite.menu.model.MenuItemEntity;
import com.quickbite.menu.repository.MenuItemRepository;
import com.quickbite.menu.service.impl.MenuOwnerServiceImpl;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

@ExtendWith(MockitoExtension.class)
class MenuOwnerServiceTest {

    @Mock
    private MenuItemRepository menuItemRepository;

    @Mock
    private CloudinaryImageService cloudinaryImageService;

    @InjectMocks
    private MenuOwnerServiceImpl menuOwnerService;

    @Test
    void addMenuItem_shouldSaveAndReturnResponse_whenRequestIsValid() {
        UUID ownerId = UUID.randomUUID();
        CreateMenuItemRequest request = new CreateMenuItemRequest(1L, null, "Paneer", "desc", 220.0, null, true, true, false);
        MockMultipartFile image = new MockMultipartFile("imageFile", "food.jpg", "image/jpeg", "img".getBytes());

        when(cloudinaryImageService.uploadMenuItemImage(image, 1L)).thenReturn("https://cdn/img.jpg");
        when(menuItemRepository.save(any(MenuItemEntity.class))).thenAnswer(invocation -> {
            MenuItemEntity entity = invocation.getArgument(0);
            entity.setId(10L);
            return entity;
        });

        var response = menuOwnerService.addMenuItem(ownerId, request, image);

        assertEquals(10L, response.id());
        assertEquals("Paneer", response.name());
        assertEquals("https://cdn/img.jpg", response.imageUrl());
        verify(menuItemRepository).save(any(MenuItemEntity.class));
    }

    @Test
    void addMenuItem_shouldThrow_whenImageMissing() {
        UUID ownerId = UUID.randomUUID();
        CreateMenuItemRequest request = new CreateMenuItemRequest(1L, null, "Paneer", "desc", 220.0, null, true, true, false);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> menuOwnerService.addMenuItem(ownerId, request, null));

        assertEquals("Menu item image is required", ex.getMessage());
    }

    @Test
    void updateMenuItem_shouldThrow_whenItemNotFound() {
        UUID ownerId = UUID.randomUUID();
        CreateMenuItemRequest request = new CreateMenuItemRequest(1L, 2L, "Paneer", "desc", 220.0, null, true, true, false);
        MultipartFile image = new MockMultipartFile("imageFile", "food.jpg", "image/jpeg", "img".getBytes());

        when(menuItemRepository.findById(99L)).thenReturn(Optional.empty());

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> menuOwnerService.updateMenuItem(99L, ownerId, request, image));

        assertEquals("Menu item not found", ex.getMessage());
    }

    @Test
    void deleteMenuItem_shouldThrow_whenItemNotFound() {
        UUID ownerId = UUID.randomUUID();
        when(menuItemRepository.existsById(90L)).thenReturn(false);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> menuOwnerService.deleteMenuItem(90L, ownerId));

        assertEquals("Menu item not found", ex.getMessage());
    }

    @Test
    void toggleAvailability_shouldUpdateFlag() {
        UUID ownerId = UUID.randomUUID();
        MenuItemEntity item = new MenuItemEntity();
        item.setId(5L);
        item.setRestaurantId(1L);
        item.setCategoryId(1L);
        item.setName("Burger");
        item.setDescription("d");
        item.setPrice(99.0);
        item.setImageUrl("img");
        item.setIsAvailable(true);

        when(menuItemRepository.findById(5L)).thenReturn(Optional.of(item));
        when(menuItemRepository.save(any(MenuItemEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var response = menuOwnerService.toggleAvailability(5L, ownerId, false);

        assertFalse(response.available());
        verify(menuItemRepository).save(any(MenuItemEntity.class));
    }
}

