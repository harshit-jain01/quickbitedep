package com.quickbite.cart.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.quickbite.cart.model.Cart;
import com.quickbite.cart.model.CartEntity;
import com.quickbite.cart.model.CartItem;
import com.quickbite.cart.model.CartItemEntity;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CartRepositoryTest {

    @Mock
    private JpaCartRepository jpaCartRepository;

    @InjectMocks
    private CartRepository cartRepository;

    @Test
    void findOrCreateByUserEmail_shouldReturnEmptyCartWhenNotFound() {
        when(jpaCartRepository.findByUserEmail("user@mail.com")).thenReturn(Optional.empty());

        Cart cart = cartRepository.findOrCreateByUserEmail("user@mail.com");

        assertEquals("user@mail.com", cart.getUserEmail());
        assertEquals(0, cart.getItems().size());
    }

    @Test
    void save_shouldThrowWhenRestaurantIdMissingWithItems() {
        Cart cart = new Cart();
        cart.setUserEmail("user@mail.com");
        CartItem item = new CartItem();
        item.setMenuItemId(10L);
        item.setItemName("Burger");
        item.setImageUrl("img");
        item.setUnitPrice(120.0);
        item.setQuantity(1);
        cart.getItems().add(item);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> cartRepository.save(cart));

        assertEquals("restaurantId is required when cart has items", ex.getMessage());
        verify(jpaCartRepository, never()).save(any());
    }

    @Test
    void save_shouldClearWhenItemsEmpty() {
        Cart cart = new Cart();
        cart.setUserEmail("user@mail.com");
        cart.setRestaurantId(1L);
        cart.setRestaurantName("R1");

        Cart saved = cartRepository.save(cart);

        assertNull(saved.getRestaurantId());
        assertNull(saved.getRestaurantName());
        verify(jpaCartRepository).deleteByUserEmail("user@mail.com");
    }

    @Test
    void save_shouldPersistAndMapItems() {
        Cart cart = new Cart();
        cart.setUserEmail("user@mail.com");
        cart.setRestaurantId(1L);
        cart.setRestaurantName("R1");
        CartItem item = new CartItem();
        item.setMenuItemId(100L);
        item.setItemName("Pizza");
        item.setImageUrl("img");
        item.setUnitPrice(200.0);
        item.setQuantity(2);
        cart.getItems().add(item);

        when(jpaCartRepository.findByUserEmail("user@mail.com")).thenReturn(Optional.empty());
        when(jpaCartRepository.save(any(CartEntity.class))).thenAnswer(invocation -> {
            CartEntity entity = invocation.getArgument(0);
            entity.setId(50L);
            entity.getItems().forEach(i -> i.setId(70L));
            return entity;
        });

        Cart saved = cartRepository.save(cart);

        assertEquals(1L, saved.getRestaurantId());
        assertEquals(1, saved.getItems().size());
        assertEquals(100L, saved.getItems().get(0).getMenuItemId());
        assertEquals(70L, saved.getItems().get(0).getId());
    }

    @Test
    void findOrCreateByUserEmail_shouldMapExistingEntity() {
        CartEntity entity = new CartEntity("user@mail.com", 1L);
        entity.setRestaurantName("R1");
        CartItemEntity item = new CartItemEntity();
        item.setId(10L);
        item.setMenuItemId(200L);
        item.setItemName("Pasta");
        item.setImageUrl("img");
        item.setUnitPrice(250.0);
        item.setQuantity(1);
        item.setCart(entity);
        entity.setItems(List.of(item));

        when(jpaCartRepository.findByUserEmail("user@mail.com")).thenReturn(Optional.of(entity));

        Cart cart = cartRepository.findOrCreateByUserEmail("user@mail.com");

        assertEquals("R1", cart.getRestaurantName());
        assertEquals(1, cart.getItems().size());
        assertEquals("Pasta", cart.getItems().get(0).getItemName());
    }
}
