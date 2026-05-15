package com.quickbite.order.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.quickbite.order.client.CartServiceClient;
import com.quickbite.order.client.DeliveryServiceClient;
import com.quickbite.order.client.PaymentServiceClient;
import com.quickbite.order.dto.CheckoutRequest;
import com.quickbite.order.dto.external.CartItemPayload;
import com.quickbite.order.dto.external.CartPayload;
import com.quickbite.order.dto.external.DeliveryAssignmentRequest;
import com.quickbite.order.dto.external.DeliveryAssignmentResponse;
import com.quickbite.order.dto.external.PaymentStatusResponse;
import com.quickbite.order.model.Order;
import com.quickbite.order.model.OrderItem;
import com.quickbite.order.repository.OrderStorageRepository;
import com.quickbite.order.service.impl.OrderServiceImpl;
import feign.FeignException;
import feign.Request;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock
    private OrderStorageRepository orderRepository;

    @Mock
    private CartServiceClient cartServiceClient;

    @Mock
    private PaymentServiceClient paymentServiceClient;

    @Mock
    private DeliveryServiceClient deliveryServiceClient;

    @Mock
    private OrderEventProducer orderEventProducer;

    @InjectMocks
    private OrderServiceImpl orderService;

    @Test
    void checkout_shouldPlaceCodOrderAssignAgentAndClearCart() {
        String userEmail = "customer@mail.com";
        CheckoutRequest request = new CheckoutRequest("Bhopal", "COD", "Leave at door", "OD1001", null, null);
        when(cartServiceClient.getCart(userEmail)).thenReturn(cart(userEmail));
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(deliveryServiceClient.assign(eq("SYSTEM"), any(DeliveryAssignmentRequest.class)))
                .thenReturn(new DeliveryAssignmentResponse("OD1001", "AGT9", "Ravi", "ASSIGNED", 25));
        when(orderRepository.updateDeliveryAgent("OD1001", "AGT9", "Ravi", 25))
                .thenReturn(order("OD1001", userEmail, "PLACED", "AGT9", "Ravi", 25));

        var response = orderService.checkout(userEmail, request);

        assertEquals("OD1001", response.orderReference());
        assertEquals("COD", response.paymentMode());
        assertEquals("PENDING", response.paymentStatus());
        assertEquals("AGT9", response.deliveryAgentId());
        verify(orderEventProducer).publishOrderCreated(any(Order.class));
        verify(cartServiceClient).clearCart(userEmail);
    }

    @Test
    void checkout_shouldContinue_whenDeliveryAssignmentFeignFails() {
        String userEmail = "customer@mail.com";
        CheckoutRequest request = new CheckoutRequest("Bhopal", "COD", null, "OD1002", null, null);
        when(cartServiceClient.getCart(userEmail)).thenReturn(cart(userEmail));
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(deliveryServiceClient.assign(eq("SYSTEM"), any(DeliveryAssignmentRequest.class)))
                .thenThrow(feignServerException());

        var response = orderService.checkout(userEmail, request);

        assertEquals("OD1002", response.orderReference());
        assertNull(response.deliveryAgentId());
        verify(orderRepository, never()).updateDeliveryAgent(any(), any(), any(), anyInt());
        verify(cartServiceClient).clearCart(userEmail);
    }

    @Test
    void checkout_shouldSupportOnlinePayments_whenVerificationMatches() {
        String userEmail = "customer@mail.com";
        CheckoutRequest request = new CheckoutRequest(
                "Address",
                "upi",
                null,
                "OD1003",
                "rzp-order",
                "rzp-payment"
        );
        when(cartServiceClient.getCart(userEmail)).thenReturn(cart(userEmail));
        when(paymentServiceClient.getPaymentStatus("OD1003")).thenReturn(new PaymentStatusResponse(
                "OD1003", "UPI", "SUCCESS", "rzp-order", "rzp-payment", "INR", 220.0
        ));
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(deliveryServiceClient.assign(eq("SYSTEM"), any(DeliveryAssignmentRequest.class)))
                .thenReturn(new DeliveryAssignmentResponse("OD1003", "", "", "UNASSIGNED", 0));

        var response = orderService.checkout(userEmail, request);

        assertEquals("UPI", response.paymentMode());
        assertEquals("SUCCESS", response.paymentStatus());
        assertEquals("rzp-order", response.razorpayOrderId());
        assertEquals("rzp-payment", response.razorpayPaymentId());
        verify(cartServiceClient).clearCart(userEmail);
    }

    @Test
    void checkout_shouldThrow_whenCartIsEmpty() {
        String userEmail = "customer@mail.com";
        when(cartServiceClient.getCart(userEmail)).thenReturn(new CartPayload(userEmail, 1L, "R", List.of(), 0, 0, 0, 0, 0));

        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> orderService.checkout(userEmail, new CheckoutRequest("Address", "COD", null, "OD2001", null, null))
        );

        assertEquals("Cart is empty", ex.getMessage());
    }

    @Test
    void checkout_shouldThrow_whenPaymentMethodUnsupported() {
        when(cartServiceClient.getCart("customer@mail.com")).thenReturn(cart("customer@mail.com"));

        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> orderService.checkout("customer@mail.com", new CheckoutRequest("Address", "WALLET", null, "OD2002", null, null))
        );

        assertEquals("Unsupported payment method", ex.getMessage());
    }

    @Test
    void checkout_shouldThrow_whenOnlinePaymentIdsMissing() {
        when(cartServiceClient.getCart("customer@mail.com")).thenReturn(cart("customer@mail.com"));

        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> orderService.checkout("customer@mail.com", new CheckoutRequest("Address", "CARD", null, "OD2003", "", null))
        );

        assertEquals("Razorpay payment details are required for online payment", ex.getMessage());
    }

    @Test
    void checkout_shouldThrow_whenPaymentRecordMissing() {
        when(cartServiceClient.getCart("customer@mail.com")).thenReturn(cart("customer@mail.com"));
        when(paymentServiceClient.getPaymentStatus("OD2004")).thenReturn(null);

        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> orderService.checkout(
                        "customer@mail.com",
                        new CheckoutRequest("Address", "UPI", null, "OD2004", "rzp-order", "rzp-payment")
                )
        );

        assertEquals("Payment record not found", ex.getMessage());
    }

    @Test
    void checkout_shouldThrow_whenPaymentNotSuccessful() {
        when(cartServiceClient.getCart("customer@mail.com")).thenReturn(cart("customer@mail.com"));
        when(paymentServiceClient.getPaymentStatus("OD2005")).thenReturn(new PaymentStatusResponse(
                "OD2005", "UPI", "FAILED", "rzp-order", "rzp-payment", "INR", 220.0
        ));

        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> orderService.checkout(
                        "customer@mail.com",
                        new CheckoutRequest("Address", "UPI", null, "OD2005", "rzp-order", "rzp-payment")
                )
        );

        assertEquals("Online payment is not verified", ex.getMessage());
    }

    @Test
    void checkout_shouldThrow_whenPaymentModeMismatches() {
        when(cartServiceClient.getCart("customer@mail.com")).thenReturn(cart("customer@mail.com"));
        when(paymentServiceClient.getPaymentStatus("OD2006")).thenReturn(new PaymentStatusResponse(
                "OD2006", "CARD", "SUCCESS", "rzp-order", "rzp-payment", "INR", 220.0
        ));

        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> orderService.checkout(
                        "customer@mail.com",
                        new CheckoutRequest("Address", "UPI", null, "OD2006", "rzp-order", "rzp-payment")
                )
        );

        assertEquals("Payment mode mismatch", ex.getMessage());
    }

    @Test
    void checkout_shouldThrow_whenPaymentIdentifiersMismatch() {
        when(cartServiceClient.getCart("customer@mail.com")).thenReturn(cart("customer@mail.com"));
        when(paymentServiceClient.getPaymentStatus("OD2007")).thenReturn(new PaymentStatusResponse(
                "OD2007", "UPI", "SUCCESS", "rzp-order-x", "rzp-payment-y", "INR", 220.0
        ));

        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> orderService.checkout(
                        "customer@mail.com",
                        new CheckoutRequest("Address", "UPI", null, "OD2007", "rzp-order", "rzp-payment")
                )
        );

        assertEquals("Payment identifiers mismatch", ex.getMessage());
    }

    @Test
    void getOrders_shouldReturnOnlyUsersOrders_forCustomerRole() {
        when(orderRepository.findByUserEmail("customer@mail.com"))
                .thenReturn(List.of(order("OD3001", "customer@mail.com", "PLACED", null, null, 30)));

        var response = orderService.getOrders("customer@mail.com", "CUSTOMER");

        assertEquals(1, response.size());
        assertEquals("OD3001", response.get(0).orderReference());
    }

    @Test
    void getOrders_shouldReturnUndeliveredOrders_forAgentRoles() {
        Order delivered = order("OD3002", "customer@mail.com", "DELIVERED", null, null, 30);
        Order active = order("OD3003", "customer@mail.com", "PICKED_UP", null, null, 30);
        when(orderRepository.findAll()).thenReturn(List.of(delivered, active));

        var response = orderService.getOrders("agent@mail.com", "AGENT");

        assertEquals(1, response.size());
        assertEquals("OD3003", response.get(0).orderReference());
    }

    @Test
    void getOrder_shouldReturnOrderByReference() {
        when(orderRepository.findByReference("customer@mail.com", "OD3004"))
                .thenReturn(order("OD3004", "customer@mail.com", "PLACED", null, null, 30));

        var response = orderService.getOrder("customer@mail.com", "OD3004");

        assertEquals("OD3004", response.orderReference());
    }

    @Test
    void getAllOrdersForAdmin_shouldReturnAllOrders() {
        when(orderRepository.findAll()).thenReturn(List.of(
                order("OD3005", "a@mail.com", "PLACED", null, null, 30),
                order("OD3006", "b@mail.com", "CONFIRMED", null, null, 35)
        ));

        var response = orderService.getAllOrdersForAdmin();

        assertEquals(2, response.size());
    }

    @Test
    void getTotalOrders_shouldReturnCountFromRepository() {
        when(orderRepository.countAll()).thenReturn(42L);
        assertEquals(42L, orderService.getTotalOrders());
    }

    @Test
    void updateOrderStatusForAdmin_shouldThrow_whenStatusIsInvalid() {
        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> orderService.updateOrderStatusForAdmin("OD4001", "DELIVERED")
        );

        assertEquals("Invalid order status", ex.getMessage());
    }

    @Test
    void updateOrderStatusForAdmin_shouldThrow_whenOrderNotFound() {
        when(orderRepository.findAll()).thenReturn(List.of());

        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> orderService.updateOrderStatusForAdmin("OD4002", "CONFIRMED")
        );

        assertEquals("Order not found", ex.getMessage());
    }

    @Test
    void updateOrderStatusForAdmin_shouldThrow_whenTransitionInvalid() {
        when(orderRepository.findAll()).thenReturn(List.of(order("OD4003", "x@mail.com", "DELIVERED", null, null, 20)));

        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> orderService.updateOrderStatusForAdmin("OD4003", "CONFIRMED")
        );

        assertEquals("Invalid status transition: DELIVERED -> CONFIRMED", ex.getMessage());
    }

    @Test
    void updateOrderStatusForAdmin_shouldUpdate_whenTransitionValid() {
        when(orderRepository.findAll()).thenReturn(List.of(order("OD4004", "x@mail.com", "PLACED", null, null, 20)));
        when(orderRepository.updateDeliveryStatus("OD4004", "CONFIRMED"))
                .thenReturn(order("OD4004", "x@mail.com", "CONFIRMED", null, null, 20));

        var response = orderService.updateOrderStatusForAdmin("OD4004", "confirmed");

        assertEquals("CONFIRMED", response.deliveryStatus());
    }

    @Test
    void updateOrderDeliveryStatusInternal_shouldThrow_whenStatusInvalid() {
        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> orderService.updateOrderDeliveryStatusInternal("OD5001", "PREPARING")
        );

        assertEquals("Invalid delivery agent status", ex.getMessage());
    }

    @Test
    void updateOrderDeliveryStatusForAgent_shouldUpdate_whenValidTransition() {
        when(orderRepository.findAll()).thenReturn(List.of(order("OD5002", "x@mail.com", "PREPARING", null, null, 20)));
        when(orderRepository.updateDeliveryStatus("OD5002", "PICKED_UP"))
                .thenReturn(order("OD5002", "x@mail.com", "PICKED_UP", null, null, 20));

        var response = orderService.updateOrderDeliveryStatusForAgent("OD5002", "picked_up");

        assertEquals("PICKED_UP", response.deliveryStatus());
        assertTrue(response.estimatedDeliveryWindow().contains("min"));
    }

    @Test
    void updateOrderDeliveryStatusForAgent_shouldAllowNoOpTransition() {
        when(orderRepository.findAll()).thenReturn(List.of(order("OD5003", "x@mail.com", "PICKED_UP", null, null, 20)));
        when(orderRepository.updateDeliveryStatus("OD5003", "PICKED_UP"))
                .thenReturn(order("OD5003", "x@mail.com", "PICKED_UP", null, null, 20));

        var response = orderService.updateOrderDeliveryStatusForAgent("OD5003", "PICKED_UP");

        assertEquals("PICKED_UP", response.deliveryStatus());
    }

    @Test
    void updateDeliveryAgentInternal_shouldApplyEtaAsProvided() {
        when(orderRepository.updateDeliveryAgent("OD5004", "AG1", "Ravi", 0))
                .thenReturn(order("OD5004", "x@mail.com", "PLACED", "AG1", "Ravi", 0));

        var response = orderService.updateDeliveryAgentInternal("OD5004", "AG1", "Ravi", 0);

        assertEquals("AG1", response.deliveryAgentId());
        assertFalse(response.estimatedDeliveryWindow().isBlank());
    }

    private CartPayload cart(String userEmail) {
        return new CartPayload(
                userEmail,
                1L,
                "Spice Route",
                List.of(new CartItemPayload(1L, 11L, "Paneer Tikka", "img", 220.0, 1, 220.0)),
                1,
                220.0,
                0,
                0,
                220.0
        );
    }

    private Order order(String orderRef, String userEmail, String deliveryStatus, String agentId, String agentName, int eta) {
        return new Order(
                orderRef,
                userEmail,
                "Spice Route",
                "Address",
                "COD",
                "PENDING",
                null,
                null,
                deliveryStatus,
                agentId,
                agentName,
                eta,
                220.0,
                "note",
                Instant.now(),
                List.of(new OrderItem("Paneer Tikka", "img", 220.0, 1, 220.0))
        );
    }

    private FeignException feignServerException() {
        Request request = Request.create(
                Request.HttpMethod.POST,
                "http://delivery-service/api/v1/agents/assign",
                Map.of(),
                null,
                StandardCharsets.UTF_8,
                null
        );
        return FeignException.errorStatus("assign", feign.Response.builder()
                .status(503)
                .reason("Service Unavailable")
                .request(request)
                .headers(Map.of())
                .build());
    }
}
