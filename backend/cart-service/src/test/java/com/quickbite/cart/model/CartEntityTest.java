package com.quickbite.cart.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class CartEntityTest {

    @Test
    void constructorAndAccessors_shouldWork() {
        CartEntity entity = new CartEntity("user@mail.com", 10L);
        entity.setId(1L);
        entity.setRestaurantName("R1");
        entity.setItems(new ArrayList<>());
        LocalDateTime now = LocalDateTime.now();
        entity.setCreatedAt(now);
        entity.setUpdatedAt(now);

        assertEquals(1L, entity.getId());
        assertEquals("user@mail.com", entity.getUserEmail());
        assertEquals(10L, entity.getRestaurantId());
        assertEquals("R1", entity.getRestaurantName());
        assertEquals(now, entity.getCreatedAt());
        assertEquals(now, entity.getUpdatedAt());
        assertNotNull(entity.getUserId());
    }

    @Test
    void setUserEmail_shouldPopulateStableUserIdWhenMissing() {
        CartEntity entity = new CartEntity();
        entity.setUserEmail("user@mail.com");
        UUID generated = entity.getUserId();

        entity.setUserEmail("user@mail.com");

        assertEquals(generated, entity.getUserId());
    }

    @Test
    void ensureLegacyUserId_shouldBackfillFromEmail() {
        CartEntity entity = new CartEntity();
        entity.setUserEmail("legacy@mail.com");
        entity.setUserId(null);

        entity.ensureLegacyUserId();

        assertNotNull(entity.getUserId());
    }
}
