package com.quickbite.cart.controller;

import com.quickbite.cart.dto.AddCartItemRequest;
import com.quickbite.cart.dto.CartResponse;
import com.quickbite.cart.dto.UpdateCartItemRequest;
import com.quickbite.cart.service.CartService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/cart")
public class CartController {

    private static final Logger logger = LoggerFactory.getLogger(CartController.class);

    private final CartService cartService;

    public CartController(CartService cartService) {
        this.cartService = cartService;
    }

    private String extractUserEmail(HttpServletRequest request, @AuthenticationPrincipal UserDetails userDetails) {
        // First check X-Authenticated-User header (from API Gateway)
        String userEmail = request.getHeader("X-Authenticated-User");
        if (userEmail != null && !userEmail.isEmpty()) {
            return userEmail;
        }

        // Fallback to authenticated user from JWT
        if (userDetails != null) {
            return userDetails.getUsername();
        }

        throw new IllegalArgumentException("User not authenticated");
    }

    @GetMapping
    public CartResponse getCart(
            HttpServletRequest request,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        String userEmail = extractUserEmail(request, userDetails);
        logger.debug("Cart fetch request for user={}", userEmail);
        return cartService.getCart(userEmail);
    }

    @PostMapping("/items")
    @ResponseStatus(HttpStatus.CREATED)
    public CartResponse addItem(
            HttpServletRequest request,
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody AddCartItemRequest requestBody
    ) {
        String userEmail = extractUserEmail(request, userDetails);
        logger.info("Add cart item request for user={} menuItemId={}", userEmail, requestBody.menuItemId());
        return cartService.addItem(userEmail, requestBody);
    }

    @PatchMapping("/items/{itemId}")
    public CartResponse updateItem(
            HttpServletRequest request,
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long itemId,
            @Valid @RequestBody UpdateCartItemRequest requestBody
    ) {
        String userEmail = extractUserEmail(request, userDetails);
        logger.info("Update cart item request for user={} itemId={}", userEmail, itemId);
        return cartService.updateItem(userEmail, itemId, requestBody);
    }

    @DeleteMapping("/items/{itemId}")
    public CartResponse removeItem(
            HttpServletRequest request,
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long itemId
    ) {
        String userEmail = extractUserEmail(request, userDetails);
        logger.info("Remove cart item request for user={} itemId={}", userEmail, itemId);
        return cartService.removeItem(userEmail, itemId);
    }

    @DeleteMapping
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void clearCart(
            HttpServletRequest request,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        String userEmail = extractUserEmail(request, userDetails);
        logger.info("Clear cart request for user={}", userEmail);
        cartService.clear(userEmail);
    }
}
