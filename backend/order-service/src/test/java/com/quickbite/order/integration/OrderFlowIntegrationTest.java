package com.quickbite.order.integration;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.quickbite.order.client.CartServiceClient;
import com.quickbite.order.client.DeliveryServiceClient;
import com.quickbite.order.client.PaymentServiceClient;
import com.quickbite.order.dto.external.CartItemPayload;
import com.quickbite.order.dto.external.CartPayload;
import com.quickbite.order.dto.external.DeliveryAssignmentRequest;
import com.quickbite.order.dto.external.DeliveryAssignmentResponse;
import com.quickbite.order.repository.OrderRepository;
import com.quickbite.order.service.OrderEventProducer;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(properties = {
        "spring.cloud.discovery.enabled=false",
        "eureka.client.enabled=false"
})
@AutoConfigureMockMvc
class OrderFlowIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private OrderRepository orderRepository;

    @MockBean
    private CartServiceClient cartServiceClient;

    @MockBean
    private PaymentServiceClient paymentServiceClient;

    @MockBean
    private DeliveryServiceClient deliveryServiceClient;

    @MockBean
    private OrderEventProducer orderEventProducer;

    @AfterEach
    void cleanUp() {
        orderRepository.deleteAll();
    }

    @Test
    void checkoutAndGetOrders_shouldPersistAndReturnData() throws Exception {
        when(cartServiceClient.getCart("integration@mail.com")).thenReturn(new CartPayload(
                "integration@mail.com",
                1L,
                "Integration Restaurant",
                List.of(new CartItemPayload(1L, 11L, "Paneer", "img", 220.0, 1, 220.0)),
                1,
                220.0,
                0,
                0,
                220.0
        ));
        when(deliveryServiceClient.assign(eq("SYSTEM"), any(DeliveryAssignmentRequest.class)))
                .thenReturn(new DeliveryAssignmentResponse("OD-INT-1", "AG1", "Ravi", "ASSIGNED", 20));
        doNothing().when(orderEventProducer).publishOrderCreated(any());
        doNothing().when(cartServiceClient).clearCart("integration@mail.com");

        mockMvc.perform(post("/api/v1/orders/checkout")
                        .header("X-Authenticated-User", "integration@mail.com")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "deliveryAddress":"Integration Street",
                                  "paymentMethod":"COD",
                                  "notes":"doorstep",
                                  "orderReference":"OD-INT-1"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.orderReference").value("OD-INT-1"))
                .andExpect(jsonPath("$.deliveryAgentId").value("AG1"));

        mockMvc.perform(get("/api/v1/orders")
                        .header("X-Authenticated-User", "integration@mail.com")
                        .header("X-Authenticated-Role", "CUSTOMER"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].orderReference").value("OD-INT-1"));
    }
}
