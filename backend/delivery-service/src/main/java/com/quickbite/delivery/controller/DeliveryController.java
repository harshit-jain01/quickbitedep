package com.quickbite.delivery.controller;

import com.quickbite.delivery.dto.AgentAssignedOrderResponse;
import com.quickbite.delivery.dto.AgentEarningsResponse;
import com.quickbite.delivery.dto.DeliveryAssignmentRequest;
import com.quickbite.delivery.dto.DeliveryAssignmentResponse;
import com.quickbite.delivery.dto.DeliveryAgentAvailabilityRequest;
import com.quickbite.delivery.dto.DeliveryAgentRegistrationRequest;
import com.quickbite.delivery.dto.DeliveryAgentResponse;
import com.quickbite.delivery.service.DeliveryService;
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
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/v1/agents")
public class DeliveryController {

    private static final Logger logger = LoggerFactory.getLogger(DeliveryController.class);

    private final DeliveryService deliveryService;

    public DeliveryController(DeliveryService deliveryService) {
        this.deliveryService = deliveryService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public DeliveryAgentResponse register(@Valid @RequestBody DeliveryAgentRegistrationRequest request) {
        logger.info("Delivery agent registration request received for phone={}", request.phone());
        return deliveryService.register(request);
    }

    @PostMapping("/test-data")
    @ResponseStatus(HttpStatus.CREATED)
    public String createTestData(@RequestHeader("X-Authenticated-Role") String role) {
        verifyRole(role, "ADMIN", "SYSTEM");
        logger.info("Creating test delivery agents data");
        
        // Create test agents
        deliveryService.register(new DeliveryAgentRegistrationRequest(
            "Rahul Kumar", "9876543210", "Bike", "MP04AB1234"
        ));
        deliveryService.register(new DeliveryAgentRegistrationRequest(
            "Priya Sharma", "9876543211", "Scooter", "DL08CD5678"
        ));
        deliveryService.register(new DeliveryAgentRegistrationRequest(
            "Amit Singh", "9876543212", "Bicycle", "HR26EF9012"
        ));
        
        return "Test data created successfully";
    }

    @GetMapping
    public List<DeliveryAgentResponse> getAllAgents(@RequestHeader("X-Authenticated-Role") String role) {
        verifyRole(role, "AGENT", "DELIVERY_AGENT", "ADMIN", "SYSTEM");
        logger.debug("Get all delivery agents request accepted for role={}", role);
        return deliveryService.getAllAgents();
    }

    @GetMapping("/me")
    public DeliveryAgentResponse getCurrentAgent(
            @RequestHeader("X-Authenticated-Role") String role,
            @RequestHeader(value = "X-Authenticated-Phone", required = false) String phone,
            @RequestHeader(value = "X-Authenticated-User", required = false) String authenticatedUser
    ) {
        verifyRole(role, "AGENT", "DELIVERY_AGENT", "ADMIN");
        logger.debug("Current delivery agent profile requested for authenticatedUser={}", authenticatedUser);
        if (phone == null || phone.isBlank()) {
            throw new IllegalArgumentException("Authenticated phone not found. Please login again.");
        }
        return deliveryService.getOrCreateAgentByPhone(phone, authenticatedUser);
    }

    @PutMapping("/{agentId}/verify")
    public DeliveryAgentResponse verifyAgent(
            @RequestHeader("X-Authenticated-Role") String role,
            @PathVariable String agentId
    ) {
        verifyRole(role, "ADMIN", "SYSTEM");
        logger.info("Delivery agent verification requested for agentId={}", agentId);
        return deliveryService.verifyAndActivate(agentId);
    }

    @PutMapping("/{agentId}/availability")
    public DeliveryAgentResponse updateAvailability(
            @RequestHeader("X-Authenticated-Role") String role,
            @PathVariable String agentId,
            @Valid @RequestBody DeliveryAgentAvailabilityRequest request
    ) {
        verifyRole(role, "AGENT", "DELIVERY_AGENT", "ADMIN");
        logger.info("Delivery availability update requested for agentId={} online={}", agentId, request.online());
        return deliveryService.updateAvailability(agentId, request);
    }

    @PostMapping("/assign")
    public DeliveryAssignmentResponse assign(
            @RequestHeader("X-Authenticated-Role") String role,
            @Valid @RequestBody DeliveryAssignmentRequest request
    ) {
        verifyRole(role, "SYSTEM", "ADMIN");
        logger.info("Delivery assignment publish request for orderReference={}", request.orderReference());
        return deliveryService.assign(request);
    }

    @GetMapping("/{agentId}/orders")
    public List<AgentAssignedOrderResponse> getAssignedOrders(
            @RequestHeader("X-Authenticated-Role") String role,
            @PathVariable String agentId
    ) {
        verifyRole(role, "AGENT", "DELIVERY_AGENT", "ADMIN");
        logger.debug("Assigned orders requested for agentId={}", agentId);
        return deliveryService.getAssignedOrders(agentId);
    }

    @PutMapping("/{agentId}/orders/{orderReference}/accept")
    public AgentAssignedOrderResponse acceptOrder(
            @RequestHeader("X-Authenticated-Role") String role,
            @PathVariable String agentId,
            @PathVariable String orderReference
    ) {
        verifyRole(role, "AGENT", "DELIVERY_AGENT", "ADMIN");
        logger.info("Accept order requested for agentId={} orderReference={}", agentId, orderReference);
        return deliveryService.acceptOrder(agentId, orderReference);
    }

    @PutMapping("/{agentId}/orders/{orderReference}/picked-up")
    public AgentAssignedOrderResponse markPickedUp(
            @RequestHeader("X-Authenticated-Role") String role,
            @PathVariable String agentId,
            @PathVariable String orderReference
    ) {
        verifyRole(role, "AGENT", "DELIVERY_AGENT", "ADMIN");
        logger.info("Mark picked-up requested for agentId={} orderReference={}", agentId, orderReference);
        return deliveryService.markPickedUp(agentId, orderReference);
    }

    @PutMapping("/{agentId}/orders/{orderReference}/delivered")
    public AgentAssignedOrderResponse markDelivered(
            @RequestHeader("X-Authenticated-Role") String role,
            @PathVariable String agentId,
            @PathVariable String orderReference
    ) {
        verifyRole(role, "AGENT", "DELIVERY_AGENT", "ADMIN");
        logger.info("Mark delivered requested for agentId={} orderReference={}", agentId, orderReference);
        return deliveryService.markDelivered(agentId, orderReference);
    }

    @GetMapping("/{agentId}/earnings")
    public AgentEarningsResponse getEarnings(
            @RequestHeader("X-Authenticated-Role") String role,
            @PathVariable String agentId
    ) {
        verifyRole(role, "AGENT", "DELIVERY_AGENT", "ADMIN");
        logger.debug("Earnings requested for agentId={}", agentId);
        return deliveryService.getEarnings(agentId);
    }

    private void verifyRole(String role, String... allowed) {
        if (role == null || role.isBlank()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Forbidden");
        }
        String normalizedRole = role.trim().toUpperCase(java.util.Locale.ROOT);
        if (normalizedRole.startsWith("ROLE_")) {
            normalizedRole = normalizedRole.substring("ROLE_".length());
        }
        for (String candidate : allowed) {
            if (candidate.equalsIgnoreCase(normalizedRole)) {
                return;
            }
        }
        throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Forbidden");
    }
}
