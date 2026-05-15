package com.quickbite.order.controller;

import com.quickbite.order.dto.AdminOrderStatusUpdateRequest;
import com.quickbite.order.dto.OrderCountResponse;
import com.quickbite.order.dto.OrderResponse;
import com.quickbite.order.service.OrderService;
import jakarta.validation.Valid;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/orders")
public class AdminOrderController {

    private static final Logger logger = LoggerFactory.getLogger(AdminOrderController.class);

    private final OrderService orderService;

    public AdminOrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @GetMapping
    public List<OrderResponse> getAllOrders(@RequestHeader("X-Authenticated-Role") String role) {
        verifyAdmin(role);
        logger.info("Admin requested full orders list");
        return orderService.getAllOrdersForAdmin();
    }

    @GetMapping("/count")
    public OrderCountResponse getTotalOrders(@RequestHeader("X-Authenticated-Role") String role) {
        verifyAdmin(role);
        logger.debug("Admin requested total orders count");
        return new OrderCountResponse(orderService.getTotalOrders());
    }

    @PutMapping("/{id}/status")
    @ResponseStatus(HttpStatus.OK)
    public OrderResponse updateOrderStatus(
            @RequestHeader("X-Authenticated-Role") String role,
            @PathVariable("id") String orderReference,
            @Valid @RequestBody AdminOrderStatusUpdateRequest request
    ) {
        verifyAdmin(role);
        logger.info("Admin updating order status for orderReference={} status={}", orderReference, request.status());
        return orderService.updateOrderStatusForAdmin(orderReference, request.status());
    }

    private void verifyAdmin(String role) {
        if (!"ADMIN".equalsIgnoreCase(role)) {
            throw new org.springframework.security.access.AccessDeniedException("Only admin can access this endpoint");
        }
    }
}

