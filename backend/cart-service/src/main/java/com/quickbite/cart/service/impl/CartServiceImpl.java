package com.quickbite.cart.service.impl;

import com.quickbite.cart.dto.AddCartItemRequest;
import com.quickbite.cart.dto.CartItemResponse;
import com.quickbite.cart.dto.CartResponse;
import com.quickbite.cart.dto.UpdateCartItemRequest;
import com.quickbite.cart.exception.ConflictException;
import com.quickbite.cart.model.Cart;
import com.quickbite.cart.model.CartItem;
import com.quickbite.cart.repository.CartRepository;
import com.quickbite.cart.service.CartService;
import java.util.Comparator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CartServiceImpl implements CartService {

    private static final Logger logger = LoggerFactory.getLogger(CartServiceImpl.class);

    private static final double DELIVERY_FEE = 39;
    private static final double TAX_RATE = 0.05;
    private static final double EXTERNAL_CHARGES_WAIVER_THRESHOLD = 150.0;

    private final CartRepository cartRepository;

    public CartServiceImpl(CartRepository cartRepository) {
        this.cartRepository = cartRepository;
    }

    @Override
    public CartResponse getCart(String userEmail) {
        logger.debug("Loading cart for user={}", userEmail);
        return toResponse(cartRepository.findOrCreateByUserEmail(userEmail));
    }

    @Override
    public CartResponse addItem(String userEmail, AddCartItemRequest request) {
        logger.info("Adding item to cart for user={} menuItemId={}", userEmail, request.menuItemId());
        Cart cart = cartRepository.findOrCreateByUserEmail(userEmail);
        if (cart.getRestaurantId() != null
                && !cart.getRestaurantId().equals(request.restaurantId())
                && !request.replaceCart()) {
            throw new ConflictException("Your cart contains items from another restaurant. Retry with replaceCart=true.");
        }

        if (request.replaceCart() || cart.getRestaurantId() == null || !cart.getRestaurantId().equals(request.restaurantId())) {
            cart.getItems().clear();
            cart.setRestaurantId(request.restaurantId());
            cart.setRestaurantName(request.restaurantName());
        }

        CartItem existing = cart.getItems().stream()
                .filter(item -> item.getMenuItemId().equals(request.menuItemId()))
                .findFirst()
                .orElse(null);

        if (existing != null) {
            existing.setQuantity(existing.getQuantity() + request.quantity());
        } else {
            CartItem item = new CartItem();
            item.setMenuItemId(request.menuItemId());
            item.setItemName(request.itemName());
            item.setImageUrl(request.imageUrl());
            item.setUnitPrice(request.unitPrice());
            item.setQuantity(request.quantity());
            cart.getItems().add(item);
        }

        Cart persistedCart = cartRepository.save(cart);
        return toResponse(persistedCart == null ? cart : persistedCart);
    }

    @Override
    public CartResponse updateItem(String userEmail, Long itemId, UpdateCartItemRequest request) {
        logger.info("Updating cart item for user={} itemId={} quantity={}", userEmail, itemId, request.quantity());
        Cart cart = cartRepository.findOrCreateByUserEmail(userEmail);
        CartItem item = cart.getItems().stream()
                .filter(cartItem -> cartItem.getId().equals(itemId))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Cart item not found"));
        item.setQuantity(request.quantity());
        Cart persistedCart = cartRepository.save(cart);
        return toResponse(persistedCart == null ? cart : persistedCart);
    }

    @Override
    @Transactional
    public CartResponse removeItem(String userEmail, Long itemId) {
        logger.info("Removing cart item for user={} itemId={}", userEmail, itemId);
        Cart cart = cartRepository.findOrCreateByUserEmail(userEmail);
        boolean removed = cart.getItems().removeIf(item -> item.getId().equals(itemId));
        if (!removed) {
            throw new IllegalArgumentException("Cart item not found");
        }
        if (cart.getItems().isEmpty()) {
            cartRepository.clear(userEmail);
            cart.setRestaurantId(null);
            cart.setRestaurantName(null);
            return toResponse(cart);
        }
        Cart persistedCart = cartRepository.save(cart);
        return toResponse(persistedCart == null ? cart : persistedCart);
    }

    @Override
    @Transactional
    public void clear(String userEmail) {
        logger.info("Clearing cart for user={}", userEmail);
        cartRepository.clear(userEmail);
    }

    private CartResponse toResponse(Cart cart) {
        double subtotal = cart.getItems().stream()
                .mapToDouble(item -> item.getUnitPrice() * item.getQuantity())
                .sum();
        boolean waiveExternalCharges = subtotal > 0 && subtotal < EXTERNAL_CHARGES_WAIVER_THRESHOLD;
        double deliveryFee = cart.getItems().isEmpty() || waiveExternalCharges ? 0 : DELIVERY_FEE;
        double taxes = waiveExternalCharges ? 0 : Math.round(subtotal * TAX_RATE * 100.0) / 100.0;
        double total = subtotal + deliveryFee + taxes;

        return new CartResponse(
                cart.getUserEmail(),
                cart.getRestaurantId(),
                cart.getRestaurantName(),
                cart.getItems().stream()
                        .sorted(Comparator.comparing(CartItem::getId))
                        .map(item -> new CartItemResponse(
                                item.getId(),
                                item.getMenuItemId(),
                                item.getItemName(),
                                item.getImageUrl(),
                                item.getUnitPrice(),
                                item.getQuantity(),
                                item.getUnitPrice() * item.getQuantity()
                        ))
                        .toList(),
                cart.getItems().stream().mapToInt(CartItem::getQuantity).sum(),
                subtotal,
                deliveryFee,
                taxes,
                total
        );
    }
}
