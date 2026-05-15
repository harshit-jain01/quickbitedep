package com.quickbite.order.controller;

import com.quickbite.order.dto.CheckoutRequest;
import com.quickbite.order.dto.OrderResponse;
import com.quickbite.order.dto.internal.DeliveryAssignmentUpdateRequest;
import com.quickbite.order.dto.internal.DeliveryStatusUpdateRequest;
import com.quickbite.order.service.OrderService;
import jakarta.validation.Valid;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/orders")
public class OrderController {

    private static final Logger logger = LoggerFactory.getLogger(OrderController.class);

    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @GetMapping
    public List<OrderResponse> getOrders(
            @RequestHeader("X-Authenticated-User") String userEmail,
            @RequestHeader("X-Authenticated-Role") String role
    ) {
        logger.info("Orders list requested by user={} role={}", userEmail, role);
        return orderService.getOrders(userEmail, role);
    }

    @GetMapping("/{orderReference}")
    public OrderResponse getOrder(
            @RequestHeader("X-Authenticated-User") String userEmail,
            @PathVariable String orderReference
    ) {
        logger.info("Order details requested for orderReference={} by user={}", orderReference, userEmail);
        return orderService.getOrder(userEmail, orderReference);
    }

    @PostMapping("/checkout")
    @ResponseStatus(HttpStatus.CREATED)
    public OrderResponse checkout(
            @RequestHeader("X-Authenticated-User") String userEmail,
            @Valid @RequestBody CheckoutRequest request
    ) {
        logger.info("Checkout request received for user={} orderReference={}", userEmail, request.orderReference());
        return orderService.checkout(userEmail, request);
    }

    @PutMapping("/internal/{orderReference}/agent")
    public OrderResponse updateAssignedAgent(
            @RequestHeader("X-Authenticated-Role") String role,
            @PathVariable String orderReference,
            @Valid @RequestBody DeliveryAssignmentUpdateRequest request
    ) {
        verifyRole(role, "SYSTEM", "ADMIN");
        logger.info("Internal delivery agent assignment update for orderReference={} agentId={}", orderReference, request.agentId());
        return orderService.updateDeliveryAgentInternal(orderReference, request.agentId(), request.agentName(), request.etaMinutes());
    }

    @PutMapping("/internal/{orderReference}/status")
    public OrderResponse updateDeliveryStatus(
            @RequestHeader("X-Authenticated-Role") String role,
            @PathVariable String orderReference,
            @Valid @RequestBody DeliveryStatusUpdateRequest request
    ) {
        verifyRole(role, "DELIVERY_AGENT", "SYSTEM", "ADMIN");
        logger.info("Internal delivery status update for orderReference={} status={}", orderReference, request.deliveryStatus());
        return orderService.updateOrderDeliveryStatusInternal(orderReference, request.deliveryStatus());
    }

    @PutMapping("/{orderReference}/delivery-status")
    public OrderResponse updateDeliveryStatusForAgent(
            @RequestHeader("X-Authenticated-Role") String role,
            @PathVariable String orderReference,
            @Valid @RequestBody DeliveryStatusUpdateRequest request
    ) {
        verifyRole(role, "AGENT", "DELIVERY_AGENT", "SYSTEM", "ADMIN");
        logger.info("Agent delivery status update for orderReference={} status={}", orderReference, request.deliveryStatus());
        return orderService.updateOrderDeliveryStatusForAgent(orderReference, request.deliveryStatus());
    }

    private void verifyRole(String role, String... allowed) {
        for (String candidate : allowed) {
            if (candidate.equalsIgnoreCase(role)) {
                return;
            }
        }
        throw new org.springframework.security.access.AccessDeniedException("Forbidden");
    }
}
