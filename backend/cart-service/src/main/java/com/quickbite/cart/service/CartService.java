package com.quickbite.cart.service;

import com.quickbite.cart.dto.AddCartItemRequest;
import com.quickbite.cart.dto.CartResponse;
import com.quickbite.cart.dto.UpdateCartItemRequest;

public interface CartService {

    CartResponse getCart(String userEmail);

    CartResponse addItem(String userEmail, AddCartItemRequest request);

    CartResponse updateItem(String userEmail, Long itemId, UpdateCartItemRequest request);

    CartResponse removeItem(String userEmail, Long itemId);

    void clear(String userEmail);
}
