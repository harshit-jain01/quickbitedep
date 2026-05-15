package com.quickbite.order.repository;

import com.quickbite.order.entity.OrderEntity;
import com.quickbite.order.entity.OrderItemEntity;
import com.quickbite.order.model.Order;
import com.quickbite.order.model.OrderItem;
import jakarta.transaction.Transactional;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Repository;

@Repository
public class OrderStorageRepository {

    private static final Logger logger = LoggerFactory.getLogger(OrderStorageRepository.class);

    private final OrderRepository orderRepository;

    public OrderStorageRepository(OrderRepository orderRepository) {
        this.orderRepository = orderRepository;
    }

    @Transactional
    public Order save(Order order) {
        logger.debug("Saving order orderReference={} userEmail={}", order.orderReference(), order.userEmail());
        return toModel(orderRepository.save(toEntity(order)));
    }

    public List<Order> findByUserEmail(String userEmail) {
        return orderRepository.findByUserEmailOrderByCreatedAtDesc(userEmail).stream()
                .map(this::toModel)
                .toList();
    }

    public Order findByReference(String userEmail, String orderReference) {
        return orderRepository.findByUserEmailAndOrderReference(userEmail, orderReference)
                .map(this::toModel)
                .orElseThrow(() -> new IllegalArgumentException("Order not found"));
    }

    public List<Order> findAll() {
        return orderRepository.findAllByOrderByCreatedAtDesc().stream()
                .map(this::toModel)
                .toList();
    }

    public long countAll() {
        return orderRepository.count();
    }

    @Transactional
    public Order updateDeliveryStatus(String orderReference, String newDeliveryStatus) {
        logger.info("Updating delivery status for orderReference={} status={}", orderReference, newDeliveryStatus);
        OrderEntity entity = orderRepository.findByOrderReference(orderReference)
                .orElseThrow(() -> new IllegalArgumentException("Order not found"));
        entity.setDeliveryStatus(newDeliveryStatus);
        return toModel(orderRepository.save(entity));
    }

    @Transactional
    public Order updateDeliveryAgent(String orderReference, String agentId, String agentName, int etaMinutes) {
        logger.info("Updating delivery agent for orderReference={} agentId={} etaMinutes={}", orderReference, agentId, etaMinutes);
        OrderEntity entity = orderRepository.findByOrderReference(orderReference)
                .orElseThrow(() -> new IllegalArgumentException("Order not found"));
        entity.setDeliveryAgentId(agentId);
        entity.setDeliveryAgent(agentName);
        entity.setEtaMinutes(etaMinutes);
        return toModel(orderRepository.save(entity));
    }


    private OrderEntity toEntity(Order order) {
        OrderEntity entity = new OrderEntity();
        entity.setOrderReference(order.orderReference());
        entity.setUserEmail(order.userEmail());
        entity.setRestaurantName(order.restaurantName());
        entity.setDeliveryAddress(order.deliveryAddress());
        entity.setPaymentMode(order.paymentMode());
        entity.setPaymentStatus(order.paymentStatus());
        entity.setRazorpayOrderId(order.razorpayOrderId());
        entity.setRazorpayPaymentId(order.razorpayPaymentId());
        entity.setDeliveryStatus(order.deliveryStatus());
        entity.setDeliveryAgentId(order.deliveryAgentId());
        entity.setDeliveryAgent(order.deliveryAgent());
        entity.setEtaMinutes(order.etaMinutes());
        entity.setTotal(order.total());
        entity.setNotes(order.notes());
        entity.setCreatedAt(order.createdAt());

        List<OrderItemEntity> itemEntities = new ArrayList<>();
        List<OrderItem> sourceItems = order.items() == null ? List.of() : order.items();
        for (OrderItem item : sourceItems) {
            OrderItemEntity itemEntity = new OrderItemEntity();
            itemEntity.setOrder(entity);
            itemEntity.setItemName(item.itemName());
            itemEntity.setImageUrl(item.imageUrl());
            itemEntity.setUnitPrice(item.unitPrice());
            itemEntity.setQuantity(item.quantity());
            itemEntity.setLineTotal(item.lineTotal());
            itemEntities.add(itemEntity);
        }
        entity.setItems(itemEntities);
        return entity;
    }

    private Order toModel(OrderEntity entity) {
        List<OrderItem> items = entity.getItems() == null ? List.of() : entity.getItems().stream()
                .map(item -> new OrderItem(
                        item.getItemName(),
                        item.getImageUrl(),
                        item.getUnitPrice(),
                        item.getQuantity(),
                        item.getLineTotal()
                ))
                .toList();

        return new Order(
                entity.getOrderReference(),
                entity.getUserEmail(),
                entity.getRestaurantName(),
                entity.getDeliveryAddress(),
                entity.getPaymentMode(),
                entity.getPaymentStatus(),
                entity.getRazorpayOrderId(),
                entity.getRazorpayPaymentId(),
                entity.getDeliveryStatus(),
                entity.getDeliveryAgentId(),
                entity.getDeliveryAgent(),
                entity.getEtaMinutes(),
                entity.getTotal(),
                entity.getNotes(),
                entity.getCreatedAt(),
                items
        );
    }
}
