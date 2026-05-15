package com.quickbite.order.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.quickbite.order.entity.OrderEntity;
import com.quickbite.order.entity.OrderItemEntity;
import com.quickbite.order.model.Order;
import com.quickbite.order.model.OrderItem;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class OrderStorageRepositoryTest {

    @Mock
    private OrderRepository orderRepository;

    @InjectMocks
    private OrderStorageRepository orderStorageRepository;

    @Test
    void save_shouldMapModelToEntityAndBack() {
        Order input = model("OD-S1", "u1@mail.com", "PLACED");
        when(orderRepository.save(any(OrderEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Order saved = orderStorageRepository.save(input);

        assertEquals("OD-S1", saved.orderReference());
        assertEquals("u1@mail.com", saved.userEmail());
        assertEquals(1, saved.items().size());
    }

    @Test
    void findByUserEmail_shouldMapEntityListToModelList() {
        when(orderRepository.findByUserEmailOrderByCreatedAtDesc("u2@mail.com"))
                .thenReturn(List.of(entity("OD-S2", "u2@mail.com", "PLACED")));

        List<Order> orders = orderStorageRepository.findByUserEmail("u2@mail.com");

        assertEquals(1, orders.size());
        assertEquals("OD-S2", orders.get(0).orderReference());
    }

    @Test
    void findByReference_shouldThrowWhenNotFound() {
        when(orderRepository.findByUserEmailAndOrderReference("u3@mail.com", "OD-S3")).thenReturn(Optional.empty());

        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> orderStorageRepository.findByReference("u3@mail.com", "OD-S3")
        );

        assertEquals("Order not found", ex.getMessage());
    }

    @Test
    void updateDeliveryStatus_shouldUpdateAndReturnMappedModel() {
        OrderEntity existing = entity("OD-S4", "u4@mail.com", "PLACED");
        when(orderRepository.findByOrderReference("OD-S4")).thenReturn(Optional.of(existing));
        when(orderRepository.save(any(OrderEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Order updated = orderStorageRepository.updateDeliveryStatus("OD-S4", "CONFIRMED");

        assertEquals("CONFIRMED", updated.deliveryStatus());
    }

    @Test
    void updateDeliveryAgent_shouldUpdateAgentFields() {
        OrderEntity existing = entity("OD-S5", "u5@mail.com", "PLACED");
        when(orderRepository.findByOrderReference("OD-S5")).thenReturn(Optional.of(existing));
        when(orderRepository.save(any(OrderEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Order updated = orderStorageRepository.updateDeliveryAgent("OD-S5", "AG1", "Ravi", 18);

        assertEquals("AG1", updated.deliveryAgentId());
        assertEquals("Ravi", updated.deliveryAgent());
        assertEquals(18, updated.etaMinutes());
    }

    @Test
    void countAll_shouldDelegateToJpaRepository() {
        when(orderRepository.count()).thenReturn(12L);
        assertEquals(12L, orderStorageRepository.countAll());
    }

    @Test
    void findAll_shouldMapEntities() {
        when(orderRepository.findAllByOrderByCreatedAtDesc())
                .thenReturn(List.of(entity("OD-S6", "u6@mail.com", "DELIVERED")));

        List<Order> orders = orderStorageRepository.findAll();

        assertEquals(1, orders.size());
        assertTrue(orders.get(0).items().size() == 1);
    }

    private Order model(String orderReference, String userEmail, String deliveryStatus) {
        return new Order(
                orderReference,
                userEmail,
                "Restaurant",
                "Address",
                "COD",
                "PENDING",
                null,
                null,
                deliveryStatus,
                null,
                null,
                30,
                220.0,
                "note",
                Instant.now(),
                List.of(new OrderItem("Item", "img", 220.0, 1, 220.0))
        );
    }

    private OrderEntity entity(String orderReference, String userEmail, String deliveryStatus) {
        OrderEntity entity = new OrderEntity();
        entity.setOrderReference(orderReference);
        entity.setUserEmail(userEmail);
        entity.setRestaurantName("Restaurant");
        entity.setDeliveryAddress("Address");
        entity.setPaymentMode("COD");
        entity.setPaymentStatus("PENDING");
        entity.setDeliveryStatus(deliveryStatus);
        entity.setEtaMinutes(30);
        entity.setTotal(220.0);
        entity.setCreatedAt(Instant.now());

        OrderItemEntity item = new OrderItemEntity();
        item.setOrder(entity);
        item.setItemName("Item");
        item.setImageUrl("img");
        item.setUnitPrice(220.0);
        item.setQuantity(1);
        item.setLineTotal(220.0);
        entity.setItems(List.of(item));
        return entity;
    }
}
