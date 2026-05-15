package com.quickbite.delivery.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class DeliveryAgentRepositoryTest {

    private final DeliveryAgentRepository repository = new DeliveryAgentRepository();

    @Test
    void registerAndFindById_shouldCreateAndLoadAgent() {
        var created = repository.register("Test Rider", "9999999990", "Bike", "MP09AB1234");

        var loaded = repository.findById(created.id());

        assertEquals("Test Rider", loaded.name());
        assertEquals("9999999990", loaded.phone());
    }

    @Test
    void findByPhone_shouldMatchNormalizedNumber() {
        var created = repository.register("Phone User", "+91 99999 12345", "Scooter", "DL08CD5678");

        var loaded = repository.findByPhone("9999912345");

        assertEquals(created.id(), loaded.id());
    }

    @Test
    void findByPhone_shouldThrowForBlankPhone() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> repository.findByPhone(" "));
        assertEquals("Delivery agent phone is required", ex.getMessage());
    }

    @Test
    void verifyAndActivate_shouldMarkAgentVerifiedAndActive() {
        var created = repository.register("Verify User", "9999999991", "Bike", "MH01AA1001");
        var updated = repository.verifyAndActivate(created.id());

        assertTrue(updated.verified());
        assertTrue(updated.active());
    }

    @Test
    void updateAvailability_shouldRejectOfflineWhenAgentHasActiveOrder() {
        var created = repository.register("Busy Rider", "9999999992", "Bike", "KA01AA1002");
        repository.reserveAgentForOrder(created.id(), "ORD-1");

        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> repository.updateAvailability(created.id(), false)
        );

        assertEquals("Agent with an active order cannot go offline", ex.getMessage());
    }

    @Test
    void reserveAvailableAgent_shouldAssignOpenOrder() {
        var reserved = repository.reserveAvailableAgent("ORD-200");
        assertEquals("ORD-200", reserved.currentOrderReference());
    }

    @Test
    void reserveAgentForOrder_shouldRejectOfflineAgent() {
        var created = repository.register("Offline Rider", "9999999993", "Bike", "TN01AA1003");
        repository.updateAvailability(created.id(), false);

        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> repository.reserveAgentForOrder(created.id(), "ORD-300")
        );
        assertEquals("Please go online before accepting orders", ex.getMessage());
    }

    @Test
    void assignmentFlow_shouldMoveThroughAcceptedPickedAndDelivered() {
        var created = repository.register("Flow Rider", "9999999994", "Bike", "RJ01AA1004");
        repository.createAssignment("ORD-400", created.id(), "R1", "A1");

        var accepted = repository.markAccepted(created.id(), "ORD-400");
        assertEquals("ACCEPTED", accepted.assignmentStatus());
        assertNotNull(accepted.acceptedAt());

        var picked = repository.markPickedUp(created.id(), "ORD-400");
        assertEquals("PICKED_UP", picked.assignmentStatus());
        assertNotNull(picked.pickedUpAt());

        var delivered = repository.markDelivered(created.id(), "ORD-400");
        assertEquals("DELIVERED", delivered.assignmentStatus());
        assertNotNull(delivered.deliveredAt());
    }

    @Test
    void findVisibleAssignmentsForAgent_shouldHideDelivered() {
        var created = repository.register("Visible Rider", "9999999995", "Bike", "UP01AA1005");
        repository.createAssignment("ORD-500", created.id(), "R1", "A1");
        repository.markAccepted(created.id(), "ORD-500");
        repository.markPickedUp(created.id(), "ORD-500");
        repository.markDelivered(created.id(), "ORD-500");

        var visible = repository.findVisibleAssignmentsForAgent(created.id());

        assertTrue(visible.stream().noneMatch(v -> "ORD-500".equals(v.orderReference())));
    }
}
