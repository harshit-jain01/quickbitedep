package com.quickbite.order.service.impl;

import com.quickbite.order.client.CartServiceClient;
import com.quickbite.order.client.DeliveryServiceClient;
import com.quickbite.order.client.PaymentServiceClient;
import com.quickbite.order.dto.CheckoutRequest;
import com.quickbite.order.dto.OrderItemResponse;
import com.quickbite.order.dto.OrderResponse;
import com.quickbite.order.dto.external.CartPayload;
import com.quickbite.order.dto.external.DeliveryAssignmentRequest;
import com.quickbite.order.dto.external.DeliveryAssignmentResponse;
import com.quickbite.order.dto.external.PaymentStatusResponse;
import com.quickbite.order.model.Order;
import com.quickbite.order.model.OrderItem;
import com.quickbite.order.repository.OrderStorageRepository;
import com.quickbite.order.service.OrderEventProducer;
import com.quickbite.order.service.OrderService;
import feign.FeignException;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ThreadLocalRandom;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OrderServiceImpl implements OrderService {

    private static final Logger logger = LoggerFactory.getLogger(OrderServiceImpl.class);

    private static final java.util.Set<String> ALLOWED_ADMIN_STATUSES = java.util.Set.of(
            "CONFIRMED",
            "PREPARING"
    );

    private static final java.util.Set<String> ALLOWED_DELIVERY_AGENT_STATUSES = java.util.Set.of(
            "PICKED_UP",
            "DELIVERED"
    );

    private final OrderStorageRepository orderRepository;
    private final CartServiceClient cartServiceClient;
    private final PaymentServiceClient paymentServiceClient;
    private final DeliveryServiceClient deliveryServiceClient;
    private final OrderEventProducer orderEventProducer;

    public OrderServiceImpl(
            OrderStorageRepository orderRepository,
            CartServiceClient cartServiceClient,
            PaymentServiceClient paymentServiceClient,
            DeliveryServiceClient deliveryServiceClient,
            OrderEventProducer orderEventProducer
    ) {
        this.orderRepository = orderRepository;
        this.cartServiceClient = cartServiceClient;
        this.paymentServiceClient = paymentServiceClient;
        this.deliveryServiceClient = deliveryServiceClient;
        this.orderEventProducer = orderEventProducer;
    }

    @Override
    @Transactional
    public OrderResponse checkout(String userEmail, CheckoutRequest request) {
        logger.info("Starting checkout for user={} orderReference={} paymentMethod={}", userEmail, request.orderReference(), request.paymentMethod());
        CartPayload cart = fetchCart(userEmail);
        if (cart.items() == null || cart.items().isEmpty()) {
            logger.warn("Checkout rejected because cart is empty for user={}", userEmail);
            throw new IllegalArgumentException("Cart is empty");
        }

        String paymentMode = normalizePaymentMode(request.paymentMethod());
        String orderReference = resolveOrderReference(request.orderReference());
        String paymentStatus;
        String razorpayOrderId = null;
        String razorpayPaymentId = null;

        if ("COD".equals(paymentMode)) {
            paymentStatus = "PENDING";
        } else if ("UPI".equals(paymentMode) || "CARD".equals(paymentMode)) {
            logger.info("Online payment verification started for orderReference={}", orderReference);
            requireOnlinePaymentIds(request);
            PaymentStatusResponse paymentDetails = fetchPaymentStatus(orderReference);
            ensureOnlinePaymentVerified(paymentDetails, paymentMode, request.razorpayOrderId(), request.razorpayPaymentId());
            paymentStatus = paymentDetails.paymentStatus();
            razorpayOrderId = paymentDetails.razorpayOrderId();
            razorpayPaymentId = paymentDetails.razorpayPaymentId();
        } else {
            throw new IllegalArgumentException("Unsupported payment method");
        }

        Order order = new Order(
                orderReference,
                userEmail,
                cart.restaurantName(),
                request.deliveryAddress(),
                paymentMode,
                paymentStatus,
                razorpayOrderId,
                razorpayPaymentId,
                "PLACED",
                null,
                null,
                30,
                cart.total(),
                request.notes(),
                Instant.now(),
                cart.items().stream()
                        .map(item -> new OrderItem(item.itemName(), item.imageUrl(), item.unitPrice(), item.quantity(), item.lineTotal()))
                        .toList()
        );
        order = orderRepository.save(order);
        orderEventProducer.publishOrderCreated(order);
        logger.info("Order persisted successfully for orderReference={} with status={}", order.orderReference(), order.deliveryStatus());
        DeliveryAssignmentResponse assignment = requestDeliveryAssignment(order);
        if (assignment != null
                && "ASSIGNED".equalsIgnoreCase(assignment.assignmentStatus())
                && assignment.agentId() != null
                && !assignment.agentId().isBlank()) {
            order = orderRepository.updateDeliveryAgent(
                    order.orderReference(),
                    assignment.agentId(),
                    assignment.agentName(),
                    Math.max(assignment.etaMinutes(), 0)
            );
            logger.info("Delivery agent assigned for orderReference={} agentId={}", order.orderReference(), assignment.agentId());
        }
        clearCart(userEmail);
        logger.debug("Cart cleared after checkout for user={}", userEmail);
        return toResponse(order);
    }

    @Override
    public List<OrderResponse> getOrders(String userEmail, String role) {
        String normalizedRole = role == null ? "" : role.trim().toUpperCase(java.util.Locale.ROOT);
        logger.debug("Fetching orders for user={} role={}", userEmail, normalizedRole);
        if ("AGENT".equals(normalizedRole) || "DELIVERY_AGENT".equals(normalizedRole)) {
            return orderRepository.findAll().stream()
                    .filter(order -> !"DELIVERED".equalsIgnoreCase(order.deliveryStatus()))
                    .sorted(java.util.Comparator.comparing(Order::createdAt).reversed())
                    .map(this::toResponse)
                    .toList();
        }

        return orderRepository.findByUserEmail(userEmail).stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    public OrderResponse getOrder(String userEmail, String orderReference) {
        logger.debug("Fetching order details for orderReference={} user={}", orderReference, userEmail);
        return toResponse(orderRepository.findByReference(userEmail, orderReference));
    }

    @Override
    public List<OrderResponse> getAllOrdersForAdmin() {
        return orderRepository.findAll().stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    public long getTotalOrders() {
        return orderRepository.countAll();
    }

    @Override
    @Transactional
    public OrderResponse updateOrderStatusForAdmin(String orderReference, String status) {
        String normalizedStatus = status == null ? "" : status.trim().toUpperCase(java.util.Locale.ROOT);
        logger.info("Admin status update requested for orderReference={} status={}", orderReference, normalizedStatus);
        if (!ALLOWED_ADMIN_STATUSES.contains(normalizedStatus)) {
            throw new IllegalArgumentException("Invalid order status");
        }
        Order existing = getOrderByReference(orderReference);
        validateTransition(existing.deliveryStatus(), normalizedStatus);
        Order updated = orderRepository.updateDeliveryStatus(orderReference, normalizedStatus);
        return toResponse(updated);
    }

    @Override
    @Transactional
    public OrderResponse updateOrderDeliveryStatusInternal(String orderReference, String status) {
        return updateOrderDeliveryStatus(orderReference, status);
    }

    @Override
    @Transactional
    public OrderResponse updateOrderDeliveryStatusForAgent(String orderReference, String status) {
        return updateOrderDeliveryStatus(orderReference, status);
    }

    @Override
    @Transactional
    public OrderResponse updateDeliveryAgentInternal(String orderReference, String agentId, String agentName, int etaMinutes) {
        logger.info("Updating assigned delivery agent for orderReference={} agentId={} etaMinutes={}", orderReference, agentId, etaMinutes);
        return toResponse(orderRepository.updateDeliveryAgent(orderReference, agentId, agentName, etaMinutes));
    }

    private OrderResponse updateOrderDeliveryStatus(String orderReference, String status) {
        String normalizedStatus = status == null ? "" : status.trim().toUpperCase(java.util.Locale.ROOT);
        logger.info("Delivery status update requested for orderReference={} status={}", orderReference, normalizedStatus);
        if (!ALLOWED_DELIVERY_AGENT_STATUSES.contains(normalizedStatus)) {
            throw new IllegalArgumentException("Invalid delivery agent status");
        }
        Order existing = getOrderByReference(orderReference);
        validateTransition(existing.deliveryStatus(), normalizedStatus);
        return toResponse(orderRepository.updateDeliveryStatus(orderReference, normalizedStatus));
    }

    private CartPayload fetchCart(String userEmail) {
        logger.debug("Fetching cart from cart-service for user={}", userEmail);
        return cartServiceClient.getCart(userEmail);
    }

    private PaymentStatusResponse fetchPaymentStatus(String orderReference) {
        logger.debug("Fetching payment status for orderReference={}", orderReference);
        return paymentServiceClient.getPaymentStatus(orderReference);
    }

    private DeliveryAssignmentResponse requestDeliveryAssignment(Order order) {
        try {
            logger.debug("Requesting delivery assignment for orderReference={}", order.orderReference());
            return deliveryServiceClient.assign("SYSTEM", new DeliveryAssignmentRequest(
                    order.orderReference(),
                    order.restaurantName(),
                    order.deliveryAddress()
            ));
        } catch (FeignException ex) {
            logger.warn("Delivery assignment request failed for orderReference={}", order.orderReference(), ex);
            return null;
        }
    }

    private void clearCart(String userEmail) {
        logger.debug("Clearing cart for user={}", userEmail);
        cartServiceClient.clearCart(userEmail);
    }

    private OrderResponse toResponse(Order order) {
        return new OrderResponse(
                order.orderReference(),
                order.orderReference(),
                order.restaurantName(),
                order.deliveryAddress(),
                order.paymentMode(),
                order.paymentStatus(),
                order.razorpayOrderId(),
                order.razorpayPaymentId(),
                order.deliveryStatus(),
                order.deliveryAgentId(),
                order.deliveryAgent(),
                order.etaMinutes(),
                formatDeliveryWindow(order.etaMinutes()),
                order.total(),
                order.notes(),
                order.createdAt(),
                order.items().stream()
                        .map(item -> new OrderItemResponse(
                                item.itemName(),
                                item.imageUrl(),
                                item.unitPrice(),
                                item.quantity(),
                                item.lineTotal()
                        ))
                        .toList()
        );
    }

    private String generateOrderReference() {
        int randomNumber = ThreadLocalRandom.current().nextInt(10_000_000, 100_000_000);
        return "OD" + randomNumber;
    }

    private String formatDeliveryWindow(int etaMinutes) {
        int lowerBound = Math.max(etaMinutes - 5, 5);
        int upperBound = etaMinutes + 5;
        return lowerBound + "-" + upperBound + " min";
    }

    private String normalizePaymentMode(String paymentMethod) {
        return paymentMethod == null ? "" : paymentMethod.trim().toUpperCase(Locale.ROOT);
    }

    private void requireOnlinePaymentIds(CheckoutRequest request) {
        if (request.razorpayOrderId() == null || request.razorpayOrderId().isBlank()
                || request.razorpayPaymentId() == null || request.razorpayPaymentId().isBlank()) {
            throw new IllegalArgumentException("Razorpay payment details are required for online payment");
        }
    }

    private void ensureOnlinePaymentVerified(
            PaymentStatusResponse payment,
            String requestedPaymentMode,
            String requestedRazorpayOrderId,
            String requestedRazorpayPaymentId
    ) {
        logger.debug("Validating payment verification payload for paymentMode={}", requestedPaymentMode);
        if (payment == null) {
            throw new IllegalArgumentException("Payment record not found");
        }
        if (!"SUCCESS".equalsIgnoreCase(payment.paymentStatus())) {
            throw new IllegalArgumentException("Online payment is not verified");
        }
        if (!requestedPaymentMode.equalsIgnoreCase(payment.paymentMode())) {
            throw new IllegalArgumentException("Payment mode mismatch");
        }
        if (!requestedRazorpayOrderId.equals(payment.razorpayOrderId())
                || !requestedRazorpayPaymentId.equals(payment.razorpayPaymentId())) {
            throw new IllegalArgumentException("Payment identifiers mismatch");
        }
    }

    private String resolveOrderReference(String requestedOrderReference) {
        if (requestedOrderReference == null || requestedOrderReference.isBlank()) {
            return generateOrderReference();
        }
        return requestedOrderReference.trim();
    }

    private Order getOrderByReference(String orderReference) {
        return orderRepository.findAll().stream()
                .filter(order -> order.orderReference().equals(orderReference))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Order not found"));
    }

    private void validateTransition(String currentStatus, String newStatus) {
        String normalizedCurrentStatus = currentStatus == null ? "" : currentStatus.trim().toUpperCase(java.util.Locale.ROOT);
        String normalizedNewStatus = newStatus == null ? "" : newStatus.trim().toUpperCase(java.util.Locale.ROOT);

        if (normalizedCurrentStatus.equals(normalizedNewStatus)) {
            return;
        }
        if ("PLACED".equals(normalizedCurrentStatus) && "CONFIRMED".equals(normalizedNewStatus)) {
            return;
        }
        if ("CONFIRMED".equals(normalizedCurrentStatus) && "PREPARING".equals(normalizedNewStatus)) {
            return;
        }
        if (("PLACED".equals(normalizedCurrentStatus) || "CONFIRMED".equals(normalizedCurrentStatus) || "PREPARING".equals(normalizedCurrentStatus))
                && "PICKED_UP".equals(normalizedNewStatus)) {
            return;
        }
        if (("PLACED".equals(normalizedCurrentStatus) || "CONFIRMED".equals(normalizedCurrentStatus) || "PREPARING".equals(normalizedCurrentStatus) || "PICKED_UP".equals(normalizedCurrentStatus))
                && "DELIVERED".equals(normalizedNewStatus)) {
            return;
        }
        throw new IllegalArgumentException("Invalid status transition: " + normalizedCurrentStatus + " -> " + normalizedNewStatus);
    }
}

