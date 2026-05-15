package com.quickbite.delivery.repository;

import com.quickbite.delivery.model.AgentOrderAssignment;
import com.quickbite.delivery.model.DeliveryAgent;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Repository;

@Repository
public class DeliveryAgentRepository {

    private static final Logger logger = LoggerFactory.getLogger(DeliveryAgentRepository.class);

    private final AtomicInteger idSequence = new AtomicInteger(1000);
    private final Map<String, DeliveryAgent> agentsById = new ConcurrentHashMap<>();
    private final Map<String, AgentOrderAssignment> assignmentsByOrder = new ConcurrentHashMap<>();

    // Initialize with sample data for testing
    public DeliveryAgentRepository() {
        initializeSampleData();
    }

    private void initializeSampleData() {
        // No sample data - agents must be registered
        logger.info("Initialized delivery agent repository with no sample agents");
    }

    public synchronized DeliveryAgent register(String name, String phone, String vehicleType, String vehicleNumber) {
        logger.info("Creating delivery agent profile for phone={}", phone);
        String id = "AGT" + idSequence.incrementAndGet();
        DeliveryAgent agent = new DeliveryAgent(
                id,
                name,
                phone,
                vehicleType,
                vehicleNumber,
                true,
                true,
                true,
                null,
                0,
                Instant.now()
        );
        agentsById.put(id, agent);
        return agent;
    }

    public List<DeliveryAgent> findAll() {
        return agentsById.values().stream()
                .sorted(Comparator.comparing(DeliveryAgent::createdAt).reversed())
                .toList();
    }

    public DeliveryAgent findById(String agentId) {
        DeliveryAgent agent = agentsById.get(agentId);
        if (agent == null) {
            throw new IllegalArgumentException("Delivery agent not found");
        }
        return agent;
    }

    public DeliveryAgent findByPhone(String phone) {
        String normalizedTarget = normalizePhone(phone);
        if (normalizedTarget.isBlank()) {
            throw new IllegalArgumentException("Delivery agent phone is required");
        }

        return agentsById.values().stream()
                .filter(agent -> phoneMatches(normalizedTarget, normalizePhone(agent.phone())))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Delivery agent not found"));
    }

    public synchronized DeliveryAgent verifyAndActivate(String agentId) {
        DeliveryAgent current = findById(agentId);
        DeliveryAgent updated = current.withVerification(true, true);
        agentsById.put(agentId, updated);
        return updated;
    }

    public synchronized DeliveryAgent updateAvailability(String agentId, boolean online) {
        logger.debug("Updating availability in repository for agentId={} online={}", agentId, online);
        DeliveryAgent current = findById(agentId);
        if (!current.active() || !current.verified()) {
            throw new IllegalArgumentException("Only verified and active delivery agents can go online");
        }
        if (current.currentOrderReference() != null && !online) {
            throw new IllegalArgumentException("Agent with an active order cannot go offline");
        }
        DeliveryAgent updated = current.withAvailability(online);
        agentsById.put(agentId, updated);
        return updated;
    }

    public synchronized DeliveryAgent reserveAvailableAgent(String orderReference) {
        DeliveryAgent selected = agentsById.values().stream()
                .filter(DeliveryAgent::verified)
                .filter(DeliveryAgent::active)
                .filter(DeliveryAgent::online)
                .filter(agent -> agent.currentOrderReference() == null)
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("No verified delivery agent is currently available"));

        DeliveryAgent reserved = selected.withCurrentOrder(orderReference);
        agentsById.put(selected.id(), reserved);
        return reserved;
    }

    public synchronized DeliveryAgent reserveAgentForOrder(String agentId, String orderReference) {
        DeliveryAgent selected = findById(agentId);
        if (!selected.verified() || !selected.active()) {
            throw new IllegalArgumentException("Only verified and active delivery agents can accept orders");
        }
        if (!selected.online()) {
            throw new IllegalArgumentException("Please go online before accepting orders");
        }
        if (selected.currentOrderReference() != null && !orderReference.equals(selected.currentOrderReference())) {
            throw new IllegalArgumentException("You already have an active order");
        }

        DeliveryAgent reserved = selected.withCurrentOrder(orderReference);
        agentsById.put(selected.id(), reserved);
        return reserved;
    }

    public synchronized void createAssignment(String orderReference, String agentId, String restaurantName, String deliveryAddress) {
        logger.info("Creating assignment entry for orderReference={} agentId={}", orderReference, agentId);
        assignmentsByOrder.put(
                orderReference,
                new AgentOrderAssignment(
                        orderReference,
                        agentId,
                        restaurantName,
                        deliveryAddress,
                        "ASSIGNED",
                        Instant.now(),
                        null,
                        null,
                        null
                )
        );
    }

    public List<AgentOrderAssignment> findAssignmentsByAgentId(String agentId) {
        List<AgentOrderAssignment> assignments = new ArrayList<>();
        for (AgentOrderAssignment assignment : assignmentsByOrder.values()) {
            if (assignment.agentId().equals(agentId)) {
                assignments.add(assignment);
            }
        }
        assignments.sort(Comparator.comparing(AgentOrderAssignment::assignedAt).reversed());
        return assignments;
    }

    public List<AgentOrderAssignment> findVisibleAssignmentsForAgent(String agentId) {
        List<AgentOrderAssignment> assignments = new ArrayList<>();
        for (AgentOrderAssignment assignment : assignmentsByOrder.values()) {
            if ("DELIVERED".equalsIgnoreCase(assignment.assignmentStatus())) {
                continue;
            }
            if (assignment.agentId() == null || assignment.agentId().isBlank() || assignment.agentId().equals(agentId)) {
                assignments.add(assignment);
            }
        }
        assignments.sort(Comparator.comparing(AgentOrderAssignment::assignedAt).reversed());
        return assignments;
    }

    public synchronized AgentOrderAssignment markAccepted(String agentId, String orderReference) {
        logger.info("Mark accepted in repository orderReference={} agentId={}", orderReference, agentId);
        AgentOrderAssignment assignment = assignmentsByOrder.get(orderReference);
        if (assignment == null) {
            throw new IllegalArgumentException("Assignment not found for this order");
        }
        if (assignment.agentId() != null && !assignment.agentId().isBlank() && !assignment.agentId().equals(agentId)) {
            throw new IllegalArgumentException("Order is already accepted by another delivery agent");
        }
        AgentOrderAssignment updated = assignment.withAgentAndStatus(
                agentId,
                "ACCEPTED",
                assignment.acceptedAt() == null ? Instant.now() : assignment.acceptedAt(),
                assignment.pickedUpAt(),
                assignment.deliveredAt()
        );
        assignmentsByOrder.put(orderReference, updated);
        return updated;
    }

    public synchronized AgentOrderAssignment markPickedUp(String agentId, String orderReference) {
        logger.info("Mark picked-up in repository orderReference={} agentId={}", orderReference, agentId);
        AgentOrderAssignment assignment = requireAssignment(agentId, orderReference);
        if (assignment.acceptedAt() == null) {
            throw new IllegalArgumentException("Order must be accepted before marking as picked up");
        }
        AgentOrderAssignment updated = assignment.withStatus("PICKED_UP", assignment.acceptedAt(), Instant.now(), assignment.deliveredAt());
        assignmentsByOrder.put(orderReference, updated);
        return updated;
    }

    public synchronized AgentOrderAssignment markDelivered(String agentId, String orderReference) {
        logger.info("Mark delivered in repository orderReference={} agentId={}", orderReference, agentId);
        AgentOrderAssignment assignment = requireAssignment(agentId, orderReference);
        if (assignment.pickedUpAt() == null) {
            throw new IllegalArgumentException("Order must be picked up before marking as delivered");
        }
        AgentOrderAssignment updated = assignment.withStatus("DELIVERED", assignment.acceptedAt(), assignment.pickedUpAt(), Instant.now());
        assignmentsByOrder.put(orderReference, updated);

        DeliveryAgent current = findById(agentId);
        DeliveryAgent completed = current.incrementCompletedDeliveries().withAvailability(true);
        agentsById.put(agentId, completed);
        return updated;
    }

    private AgentOrderAssignment requireAssignment(String agentId, String orderReference) {
        AgentOrderAssignment assignment = assignmentsByOrder.get(orderReference);
        if (assignment == null || assignment.agentId() == null || !assignment.agentId().equals(agentId)) {
            throw new IllegalArgumentException("Assignment not found for this delivery agent and order");
        }
        return assignment;
    }

    private boolean phoneMatches(String source, String target) {
        if (source.equals(target)) {
            return true;
        }
        if (source.length() >= 10 && target.length() >= 10) {
            return source.substring(source.length() - 10).equals(target.substring(target.length() - 10));
        }
        return false;
    }

    private String normalizePhone(String value) {
        if (value == null) {
            return "";
        }
        return value.replaceAll("[^0-9]", "");
    }
}

