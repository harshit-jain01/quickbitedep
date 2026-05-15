package com.quickbite.menu.repository;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class MenuRepositoryTest {

    private final MenuRepository repository = new MenuRepository();

    @Test
    void findByRestaurantId_shouldReturnStaticMenuForKnownRestaurant() {
        var items = repository.findByRestaurantId(1L);
        assertFalse(items.isEmpty());
    }

    @Test
    void findByRestaurantId_shouldReturnEmptyForUnknownRestaurant() {
        var items = repository.findByRestaurantId(999L);
        assertTrue(items.isEmpty());
    }
}
