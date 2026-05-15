package com.quickbite.menu.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

import com.quickbite.menu.model.MenuItem;
import com.quickbite.menu.model.MenuItemEntity;
import com.quickbite.menu.repository.MenuItemRepository;
import com.quickbite.menu.repository.MenuRepository;
import com.quickbite.menu.service.impl.MenuServiceImpl;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class MenuServiceImplTest {

    @Mock
    private MenuRepository menuRepository;

    @Mock
    private MenuItemRepository menuItemRepository;

    @InjectMocks
    private MenuServiceImpl menuService;

    @Test
    void getRestaurantMenu_shouldPreferDynamicItemsAndGroupByCategory() {
        MenuItemEntity item = new MenuItemEntity();
        item.setId(1L);
        item.setRestaurantId(5L);
        item.setCategoryId(2L);
        item.setName("Paneer");
        item.setDescription("d");
        item.setPrice(120.0);
        item.setIsVegetarian(true);
        item.setIsBestseller(false);
        item.setImageUrl("img");

        when(menuItemRepository.findByRestaurantIdAndIsAvailableTrue(5L)).thenReturn(List.of(item));
        when(menuItemRepository.findByRestaurantIdAndIsAvailableTrue(1_000_005L)).thenReturn(List.of());

        var groups = menuService.getRestaurantMenu(5L);

        assertEquals(1, groups.size());
        assertEquals("Main Course", groups.get(0).category());
        assertEquals(1, groups.get(0).items().size());
    }

    @Test
    void getRestaurantMenu_shouldFallbackToStaticRepositoryWhenDynamicEmpty() {
        when(menuItemRepository.findByRestaurantIdAndIsAvailableTrue(2L)).thenReturn(List.of());
        when(menuItemRepository.findByRestaurantIdAndIsAvailableTrue(1_000_002L)).thenReturn(List.of());
        when(menuRepository.findByRestaurantId(2L)).thenReturn(List.of(
                new MenuItem(11L, 2L, "Biryani", "desc", "Biryani", 250.0, false, true, "img")
        ));

        var groups = menuService.getRestaurantMenu(2L);

        assertEquals(1, groups.size());
        assertEquals("Biryani", groups.get(0).category());
        assertEquals("Biryani", groups.get(0).items().get(0).name());
    }

    @Test
    void getRestaurantMenu_shouldReturnEmptyForInvalidRestaurantId() {
        assertEquals(0, menuService.getRestaurantMenu(null).size());
        assertEquals(0, menuService.getRestaurantMenu(0L).size());
    }

    @Test
    void getRestaurantMenu_shouldIgnoreRuntimeFailuresFromOneCandidateId() {
        MenuItemEntity item = new MenuItemEntity();
        item.setId(9L);
        item.setRestaurantId(1_000_010L);
        item.setCategoryId(9L);
        item.setName("Chef Special");
        item.setDescription("d");
        item.setPrice(99.0);
        item.setImageUrl("img");
        item.setIsVegetarian(false);
        item.setIsBestseller(true);

        when(menuItemRepository.findByRestaurantIdAndIsAvailableTrue(10L)).thenThrow(new RuntimeException("db issue"));
        when(menuItemRepository.findByRestaurantIdAndIsAvailableTrue(1_000_010L)).thenReturn(List.of(item));

        var groups = menuService.getRestaurantMenu(10L);

        assertEquals(1, groups.size());
        assertEquals("Category 9", groups.get(0).category());
    }
}
