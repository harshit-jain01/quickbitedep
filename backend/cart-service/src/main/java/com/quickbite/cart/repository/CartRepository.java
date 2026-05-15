package com.quickbite.cart.repository;

import com.quickbite.cart.model.CartEntity;
import com.quickbite.cart.model.Cart;
import com.quickbite.cart.model.CartItem;
import com.quickbite.cart.model.CartItemEntity;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Repository;

@Repository
public class CartRepository {

    private static final Logger logger = LoggerFactory.getLogger(CartRepository.class);

    private final JpaCartRepository jpaCartRepository;

    public CartRepository(JpaCartRepository jpaCartRepository) {
        this.jpaCartRepository = jpaCartRepository;
    }

    public Cart findOrCreateByUserEmail(String userEmail) {
        logger.debug("Finding or creating cart for user={}", userEmail);
        return jpaCartRepository.findByUserEmail(userEmail)
                .map(this::toDomain)
                .orElseGet(() -> emptyCart(userEmail));
    }

    public Cart save(Cart cart) {
        if (cart.getItems().isEmpty()) {
            clear(cart.getUserEmail());
            cart.setRestaurantId(null);
            cart.setRestaurantName(null);
            return cart;
        }

        if (cart.getRestaurantId() == null) {
            throw new IllegalArgumentException("restaurantId is required when cart has items");
        }

        CartEntity entity = jpaCartRepository.findByUserEmail(cart.getUserEmail())
                .orElseGet(() -> new CartEntity(cart.getUserEmail(), cart.getRestaurantId()));

        // Keep legacy non-null user_id aligned for older carts schema.
        entity.setUserEmail(cart.getUserEmail());
        entity.setRestaurantId(cart.getRestaurantId());
        entity.setRestaurantName(cart.getRestaurantName());

        Map<Long, CartItemEntity> existingById = new HashMap<>();
        for (CartItemEntity itemEntity : entity.getItems()) {
            if (itemEntity.getId() != null) {
                existingById.put(itemEntity.getId(), itemEntity);
            }
        }

        List<CartItemEntity> merged = cart.getItems().stream()
                .map(item -> toItemEntity(item, existingById))
                .toList();

        entity.getItems().clear();
        for (CartItemEntity itemEntity : merged) {
            itemEntity.setCart(entity);
            entity.getItems().add(itemEntity);
        }

        return toDomain(jpaCartRepository.save(entity));
    }

    public void clear(String userEmail) {
        logger.debug("Removing cart entry for user={}", userEmail);
        jpaCartRepository.deleteByUserEmail(userEmail);
    }

    private Cart toDomain(CartEntity entity) {
        Cart cart = new Cart();
        cart.setUserEmail(entity.getUserEmail());
        cart.setRestaurantId(entity.getRestaurantId());
        cart.setRestaurantName(entity.getRestaurantName());
        cart.getItems().addAll(entity.getItems().stream().map(this::toItem).toList());
        return cart;
    }

    private CartItem toItem(CartItemEntity entity) {
        CartItem item = new CartItem();
        item.setId(entity.getId());
        item.setMenuItemId(entity.getMenuItemId());
        item.setItemName(entity.getItemName());
        item.setImageUrl(entity.getImageUrl());
        item.setUnitPrice(entity.getUnitPrice());
        item.setQuantity(entity.getQuantity());
        return item;
    }

    private CartItemEntity toItemEntity(CartItem item, Map<Long, CartItemEntity> existingById) {
        CartItemEntity entity = item.getId() != null ? existingById.getOrDefault(item.getId(), new CartItemEntity()) : new CartItemEntity();
        entity.setMenuItemId(item.getMenuItemId());
        entity.setItemName(item.getItemName());
        entity.setImageUrl(item.getImageUrl());
        entity.setUnitPrice(item.getUnitPrice());
        entity.setQuantity(item.getQuantity());
        return entity;
    }

    private Cart emptyCart(String userEmail) {
        Cart cart = new Cart();
        cart.setUserEmail(userEmail);
        return cart;
    }
}
