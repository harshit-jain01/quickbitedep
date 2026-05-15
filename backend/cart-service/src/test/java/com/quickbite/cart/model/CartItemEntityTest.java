package com.quickbite.cart.model;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class CartItemEntityTest {

    @Test
    void accessors_shouldReadAndWriteAllFields() {
        CartEntity cart = new CartEntity("user@mail.com", 1L);
        CartItemEntity item = new CartItemEntity();
        item.setId(11L);
        item.setCart(cart);
        item.setMenuItemId(21L);
        item.setItemName("Burger");
        item.setImageUrl("img");
        item.setUnitPrice(99.5);
        item.setQuantity(2);

        assertEquals(11L, item.getId());
        assertEquals(cart, item.getCart());
        assertEquals(21L, item.getMenuItemId());
        assertEquals("Burger", item.getItemName());
        assertEquals("img", item.getImageUrl());
        assertEquals(99.5, item.getUnitPrice());
        assertEquals(2, item.getQuantity());
    }
}
