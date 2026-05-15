package com.quickbite.delivery.service.impl;

import com.quickbite.delivery.client.OrderInternalClient;
import com.quickbite.delivery.dto.AgentAssignedOrderResponse;
import com.quickbite.delivery.dto.AgentEarningsResponse;
import com.quickbite.delivery.dto.DeliveryAssignmentRequest;
import com.quickbite.delivery.dto.DeliveryAssignmentResponse;
import com.quickbite.delivery.dto.DeliveryAgentAvailabilityRequest;
import com.quickbite.delivery.dto.DeliveryAgentRegistrationRequest;
import com.quickbite.delivery.dto.DeliveryAgentResponse;
import com.quickbite.delivery.dto.internal.DeliveryAssignmentUpdateRequest;
import com.quickbite.delivery.dto.internal.DeliveryStatusUpdateRequest;
import com.quickbite.delivery.repository.DeliveryAgentRepository;
import com.quickbite.delivery.service.DeliveryService;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class DeliveryServiceImpl implements DeliveryService {

    private static final Logger logger = LoggerFactory.getLogger(DeliveryServiceImpl.class);

    private static final double EARNING_PER_DELIVERY = 40.0;

    private final DeliveryAgentRepository deliveryAgentRepository;
    private final OrderInternalClient orderInternalClient;

    public DeliveryServiceImpl(DeliveryAgentRepository deliveryAgentRepository, OrderInternalClient orderInternalClient) {
        this.deliveryAgentRepository = deliveryAgentRepository;
        this.orderInternalClient = orderInternalClient;
    }

    @Override
    public DeliveryAgentResponse register(DeliveryAgentRegistrationRequest request) {
        logger.info("Registering delivery agent for phone={}", request.phone());
        return toAgentResponse(deliveryAgentRepository.register(
                request.name().trim(),
                request.phone().trim(),
                request.vehicleType().trim(),
                request.vehicleNumber().trim()
        ));
    }

    @Override
    public List<DeliveryAgentResponse> getAllAgents() {
        logger.debug("Fetching all delivery agents");
        return deliveryAgentRepository.findAll().stream()
                .map(this::toAgentResponse)
                .toList();
    }

    @Override
    public DeliveryAgentResponse verifyAndActivate(String agentId) {
        logger.info("Verifying and activating agentId={}", agentId);
        return toAgentResponse(deliveryAgentRepository.verifyAndActivate(agentId));
    }

    @Override
    public DeliveryAgentResponse getAgentByPhone(String phone) {
        return toAgentResponse(deliveryAgentRepository.findByPhone(phone));
    }

    @Override
    public DeliveryAgentResponse getOrCreateAgentByPhone(String phone, String authenticatedUser) {
        logger.debug("Fetching or creating delivery agent by phone");
        try {
            return toAgentResponse(deliveryAgentRepository.findByPhone(phone));
        } catch (IllegalArgumentException ex) {
            if (!"Delivery agent not found".equalsIgnoreCase(ex.getMessage())) {
                throw ex;
            }

            String normalizedPhone = phone == null ? "" : phone.trim();
            if (normalizedPhone.isBlank()) {
                logger.warn("Cannot resolve delivery agent profile because authenticated phone is blank");
                throw new IllegalArgumentException("Authenticated phone not found. Please login again.");
            }

            throw new IllegalArgumentException("Delivery agent not found. Please register with your vehicle details first.");
        }
    }

    @Override
    public DeliveryAgentResponse updateAvailability(String agentId, DeliveryAgentAvailabilityRequest request) {
        logger.info("Updating availability for agentId={} online={}", agentId, request.online());
        return toAgentResponse(deliveryAgentRepository.updateAvailability(agentId, request.online()));
    }

    @Override
    public DeliveryAssignmentResponse assign(DeliveryAssignmentRequest request) {
        logger.info("Publishing order for delivery assignment orderReference={}", request.orderReference());
        deliveryAgentRepository.createAssignment(
                request.orderReference(),
                null,
                request.restaurantName(),
                request.deliveryAddress()
        );
        return new DeliveryAssignmentResponse(
                request.orderReference(),
                null,
                null,
                "PENDING_ASSIGNMENT",
                0
        );
    }

    @Override
    public List<AgentAssignedOrderResponse> getAssignedOrders(String agentId) {
        logger.debug("Fetching visible assignments for agentId={}", agentId);
        return deliveryAgentRepository.findVisibleAssignmentsForAgent(agentId).stream()
                .map(assignment -> new AgentAssignedOrderResponse(
                        assignment.orderReference(),
                        assignment.restaurantName(),
                        assignment.deliveryAddress(),
                        assignment.assignmentStatus(),
                        assignment.assignedAt(),
                        assignment.acceptedAt(),
                        assignment.pickedUpAt(),
                        assignment.deliveredAt()
                ))
                .toList();
    }

    @Override
    public AgentAssignedOrderResponse acceptOrder(String agentId, String orderReference) {
        logger.info("Accepting order orderReference={} agentId={}", orderReference, agentId);
        var reservedAgent = deliveryAgentRepository.reserveAgentForOrder(agentId, orderReference);
        var assignment = deliveryAgentRepository.markAccepted(agentId, orderReference);
        updateOrderAgent(orderReference, agentId, reservedAgent.name(), ThreadLocalRandom.current().nextInt(18, 36));
        return new AgentAssignedOrderResponse(
                assignment.orderReference(),
                assignment.restaurantName(),
                assignment.deliveryAddress(),
                assignment.assignmentStatus(),
                assignment.assignedAt(),
                assignment.acceptedAt(),
                assignment.pickedUpAt(),
                assignment.deliveredAt()
        );
    }

    @Override
    public AgentAssignedOrderResponse markPickedUp(String agentId, String orderReference) {
        logger.info("Marking picked-up orderReference={} agentId={}", orderReference, agentId);
        var assignment = deliveryAgentRepository.markPickedUp(agentId, orderReference);
        updateOrderStatus(orderReference, "PICKED_UP");
        return new AgentAssignedOrderResponse(
                assignment.orderReference(),
                assignment.restaurantName(),
                assignment.deliveryAddress(),
                assignment.assignmentStatus(),
                assignment.assignedAt(),
                assignment.acceptedAt(),
                assignment.pickedUpAt(),
                assignment.deliveredAt()
        );
    }

    @Override
    public AgentAssignedOrderResponse markDelivered(String agentId, String orderReference) {
        logger.info("Marking delivered orderReference={} agentId={}", orderReference, agentId);
        var assignment = deliveryAgentRepository.markDelivered(agentId, orderReference);
        updateOrderStatus(orderReference, "DELIVERED");
        return new AgentAssignedOrderResponse(
                assignment.orderReference(),
                assignment.restaurantName(),
                assignment.deliveryAddress(),
                assignment.assignmentStatus(),
                assignment.assignedAt(),
                assignment.acceptedAt(),
                assignment.pickedUpAt(),
                assignment.deliveredAt()
        );
    }

    @Override
    public AgentEarningsResponse getEarnings(String agentId) {
        logger.debug("Fetching earnings for agentId={}", agentId);
        var agent = deliveryAgentRepository.findById(agentId);
        return new AgentEarningsResponse(
                agent.id(),
                agent.completedDeliveries(),
                agent.completedDeliveries() * EARNING_PER_DELIVERY
        );
    }

    private void updateOrderAgent(String orderReference, String agentId, String agentName, int etaMinutes) {
        logger.debug("Updating order-service assignment orderReference={} agentId={} etaMinutes={}", orderReference, agentId, etaMinutes);
        orderInternalClient.updateAgent(
                "SYSTEM",
                orderReference,
                new DeliveryAssignmentUpdateRequest(agentId, agentName, etaMinutes)
        );
    }

    private void updateOrderStatus(String orderReference, String deliveryStatus) {
        logger.debug("Updating order-service delivery status orderReference={} status={}", orderReference, deliveryStatus);
        orderInternalClient.updateStatus(
                "DELIVERY_AGENT",
                orderReference,
                new DeliveryStatusUpdateRequest(deliveryStatus)
        );
    }

    private DeliveryAgentResponse toAgentResponse(com.quickbite.delivery.model.DeliveryAgent agent) {
        return new DeliveryAgentResponse(
                agent.id(),
                agent.name(),
                agent.phone(),
                agent.vehicleType(),
                agent.vehicleNumber(),
                agent.verified(),
                agent.active(),
                agent.online(),
                agent.completedDeliveries()
        );
    }

    private String deriveDefaultName(String authenticatedUser) {
        if (authenticatedUser == null || authenticatedUser.isBlank()) {
            return "Delivery Agent";
        }
        String localPart = authenticatedUser.split("@", 2)[0].trim();
        if (localPart.isBlank()) {
            return "Delivery Agent";
        }
        String sanitized = localPart.replaceAll("[^a-zA-Z0-9 ]", " ").trim();
        if (sanitized.isBlank()) {
            return "Delivery Agent";
        }
        return Character.toUpperCase(sanitized.charAt(0)) + sanitized.substring(1);
    }
}

