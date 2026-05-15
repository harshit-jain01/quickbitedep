package com.quickbite.order.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.quickbite.order.entity.OrderEntity;
import com.quickbite.order.entity.OrderItemEntity;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

@DataJpaTest
class OrderRepositoryTest {

    @Autowired
    private OrderRepository orderRepository;

    @Test
    void findByUserEmailOrderByCreatedAtDesc_shouldReturnNewestFirst() {
        orderRepository.save(orderEntity("OD-R1", "u1@mail.com", Instant.parse("2026-01-01T10:00:00Z")));
        orderRepository.save(orderEntity("OD-R2", "u1@mail.com", Instant.parse("2026-01-01T12:00:00Z")));

        List<OrderEntity> orders = orderRepository.findByUserEmailOrderByCreatedAtDesc("u1@mail.com");

        assertEquals(2, orders.size());
        assertEquals("OD-R2", orders.get(0).getOrderReference());
        assertEquals("OD-R1", orders.get(1).getOrderReference());
    }

    @Test
    void findByUserEmailAndOrderReference_shouldReturnMatchingOrder() {
        orderRepository.save(orderEntity("OD-R3", "u2@mail.com", Instant.now()));

        var result = orderRepository.findByUserEmailAndOrderReference("u2@mail.com", "OD-R3");

        assertTrue(result.isPresent());
        assertEquals("u2@mail.com", result.get().getUserEmail());
    }

    @Test
    void findByOrderReference_shouldReturnOrderWithItems() {
        orderRepository.save(orderEntity("OD-R4", "u3@mail.com", Instant.now()));

        var result = orderRepository.findByOrderReference("OD-R4");

        assertTrue(result.isPresent());
        assertEquals(1, result.get().getItems().size());
    }

    @Test
    void findAllByOrderByCreatedAtDesc_shouldSortAcrossUsers() {
        orderRepository.save(orderEntity("OD-R5", "u4@mail.com", Instant.parse("2026-01-01T08:00:00Z")));
        orderRepository.save(orderEntity("OD-R6", "u5@mail.com", Instant.parse("2026-01-01T09:00:00Z")));

        List<OrderEntity> orders = orderRepository.findAllByOrderByCreatedAtDesc();

        assertEquals(2, orders.size());
        assertEquals("OD-R6", orders.get(0).getOrderReference());
        assertEquals("OD-R5", orders.get(1).getOrderReference());
    }

    private OrderEntity orderEntity(String reference, String userEmail, Instant createdAt) {
        OrderEntity order = new OrderEntity();
        order.setOrderReference(reference);
        order.setUserEmail(userEmail);
        order.setRestaurantName("Repo Restaurant");
        order.setDeliveryAddress("Repo Address");
        order.setPaymentMode("COD");
        order.setPaymentStatus("PENDING");
        order.setDeliveryStatus("PLACED");
        order.setEtaMinutes(30);
        order.setTotal(200.0);
        order.setNotes("note");
        order.setCreatedAt(createdAt);

        OrderItemEntity item = new OrderItemEntity();
        item.setOrder(order);
        item.setItemName("Item");
        item.setImageUrl("img");
        item.setUnitPrice(100.0);
        item.setQuantity(2);
        item.setLineTotal(200.0);
        order.setItems(List.of(item));
        return order;
    }
}
