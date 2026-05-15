package com.quickbite.cart.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.quickbite.cart.dto.AddCartItemRequest;
import com.quickbite.cart.dto.UpdateCartItemRequest;
import com.quickbite.cart.exception.ConflictException;
import com.quickbite.cart.model.Cart;
import com.quickbite.cart.model.CartItem;
import com.quickbite.cart.repository.CartRepository;
import com.quickbite.cart.service.impl.CartServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CartServiceTest {

    @Mock
    private CartRepository cartRepository;

    @InjectMocks
    private CartServiceImpl cartService;

    @Test
    void addItem_shouldAddNewItem_whenRestaurantMatches() {
        Cart cart = cart();
        when(cartRepository.findOrCreateByUserEmail("user@mail.com")).thenReturn(cart);

        AddCartItemRequest request = new AddCartItemRequest(1L, "R1", 101L, "Burger", "img", 120.0, 2, false);

        var response = cartService.addItem("user@mail.com", request);

        assertEquals(1, response.items().size());
        assertEquals(2, response.itemCount());
        assertEquals(240.0, response.subtotal());
    }

    @Test
    void addItem_shouldThrowConflict_whenRestaurantDiffAndReplaceFalse() {
        Cart cart = cart();
        when(cartRepository.findOrCreateByUserEmail("user@mail.com")).thenReturn(cart);

        AddCartItemRequest request = new AddCartItemRequest(2L, "R2", 101L, "Burger", "img", 120.0, 1, false);

        ConflictException ex = assertThrows(ConflictException.class, () -> cartService.addItem("user@mail.com", request));

        assertEquals("Your cart contains items from another restaurant. Retry with replaceCart=true.", ex.getMessage());
    }

    @Test
    void removeItem_shouldThrow_whenItemNotInCart() {
        Cart cart = cart();
        when(cartRepository.findOrCreateByUserEmail("user@mail.com")).thenReturn(cart);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> cartService.removeItem("user@mail.com", 99L));

        assertEquals("Cart item not found", ex.getMessage());
    }

    @Test
    void updateItem_shouldChangeQuantity_whenItemExists() {
        Cart cart = cart();
        CartItem item = new CartItem();
        item.setId(1L);
        item.setMenuItemId(10L);
        item.setItemName("Pizza");
        item.setImageUrl("img");
        item.setUnitPrice(200.0);
        item.setQuantity(1);
        cart.getItems().add(item);

        when(cartRepository.findOrCreateByUserEmail("user@mail.com")).thenReturn(cart);

        var response = cartService.updateItem("user@mail.com", 1L, new UpdateCartItemRequest(3));

        assertEquals(3, response.itemCount());
        assertEquals(600.0, response.subtotal());
    }

    @Test
    void clear_shouldDelegateToRepository() {
        cartService.clear("user@mail.com");
        verify(cartRepository).clear("user@mail.com");
    }

    @Test
    void removeItem_shouldClearRepository_whenRemovingLastItem() {
        Cart cart = cart();
        CartItem item = new CartItem();
        item.setId(7L);
        item.setMenuItemId(101L);
        item.setItemName("Burger");
        item.setImageUrl("img");
        item.setUnitPrice(120.0);
        item.setQuantity(1);
        cart.getItems().add(item);

        when(cartRepository.findOrCreateByUserEmail("user@mail.com")).thenReturn(cart);

        var response = cartService.removeItem("user@mail.com", 7L);

        assertEquals(0, response.itemCount());
        assertEquals(null, response.restaurantId());
        verify(cartRepository).clear("user@mail.com");
        verify(cartRepository, never()).save(any());
    }

    private Cart cart() {
        Cart cart = new Cart();
        cart.setUserEmail("user@mail.com");
        cart.setRestaurantId(1L);
        cart.setRestaurantName("R1");
        return cart;
    }
}

