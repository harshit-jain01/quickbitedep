package com.quickbite.order.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.quickbite.order.dto.OrderItemResponse;
import com.quickbite.order.dto.OrderResponse;
import com.quickbite.order.exception.GlobalExceptionHandler;
import com.quickbite.order.service.OrderService;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(controllers = OrderController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class OrderControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private OrderService orderService;

    @Test
    void getOrders_shouldReturnOrdersList() throws Exception {
        when(orderService.getOrders("customer@mail.com", "CUSTOMER")).thenReturn(List.of(orderResponse("OD1001")));

        mockMvc.perform(get("/api/v1/orders")
                        .header("X-Authenticated-User", "customer@mail.com")
                        .header("X-Authenticated-Role", "CUSTOMER"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].orderReference").value("OD1001"));

        verify(orderService).getOrders("customer@mail.com", "CUSTOMER");
    }

    @Test
    void getOrder_shouldReturnOrderDetails() throws Exception {
        when(orderService.getOrder("customer@mail.com", "OD1002")).thenReturn(orderResponse("OD1002"));

        mockMvc.perform(get("/api/v1/orders/OD1002")
                        .header("X-Authenticated-User", "customer@mail.com"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.orderReference").value("OD1002"));

        verify(orderService).getOrder("customer@mail.com", "OD1002");
    }

    @Test
    void checkout_shouldReturn201_whenRequestIsValid() throws Exception {
        when(orderService.checkout(eq("customer@mail.com"), any())).thenReturn(orderResponse("OD1003"));

        mockMvc.perform(post("/api/v1/orders/checkout")
                        .header("X-Authenticated-User", "customer@mail.com")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "deliveryAddress":"MP Nagar",
                                  "paymentMethod":"COD",
                                  "notes":"Leave at gate",
                                  "orderReference":"OD1003"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.orderReference").value("OD1003"));

        verify(orderService).checkout(eq("customer@mail.com"), any());
    }

    @Test
    void checkout_shouldReturn400_whenRequestIsInvalid() throws Exception {
        mockMvc.perform(post("/api/v1/orders/checkout")
                        .header("X-Authenticated-User", "customer@mail.com")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "deliveryAddress":"",
                                  "paymentMethod":""
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").exists());

        verify(orderService, never()).checkout(eq("customer@mail.com"), any());
    }

    @Test
    void updateAssignedAgent_shouldReturn200_whenRoleAllowed() throws Exception {
        when(orderService.updateDeliveryAgentInternal("OD1004", "AGT7", "Ravi", 22)).thenReturn(orderResponse("OD1004"));

        mockMvc.perform(put("/api/v1/orders/internal/OD1004/agent")
                        .header("X-Authenticated-Role", "SYSTEM")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "agentId":"AGT7",
                                  "agentName":"Ravi",
                                  "etaMinutes":22
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.orderReference").value("OD1004"));

        verify(orderService).updateDeliveryAgentInternal("OD1004", "AGT7", "Ravi", 22);
    }

    @Test
    void updateAssignedAgent_shouldReturn403_whenRoleForbidden() throws Exception {
        mockMvc.perform(put("/api/v1/orders/internal/OD1005/agent")
                        .header("X-Authenticated-Role", "CUSTOMER")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "agentId":"AGT7",
                                  "agentName":"Ravi",
                                  "etaMinutes":22
                                }
                                """))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("Forbidden"));

        verify(orderService, never()).updateDeliveryAgentInternal(any(), any(), any(), anyInt());
    }

    @Test
    void updateDeliveryStatusInternal_shouldReturn200_whenRoleAllowed() throws Exception {
        when(orderService.updateOrderDeliveryStatusInternal("OD1006", "DELIVERED")).thenReturn(orderResponse("OD1006"));

        mockMvc.perform(put("/api/v1/orders/internal/OD1006/status")
                        .header("X-Authenticated-Role", "DELIVERY_AGENT")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "deliveryStatus":"DELIVERED"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.orderReference").value("OD1006"));

        verify(orderService).updateOrderDeliveryStatusInternal("OD1006", "DELIVERED");
    }

    @Test
    void updateDeliveryStatusForAgent_shouldReturn403_whenRoleForbidden() throws Exception {
        mockMvc.perform(put("/api/v1/orders/OD1007/delivery-status")
                        .header("X-Authenticated-Role", "CUSTOMER")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "deliveryStatus":"DELIVERED"
                                }
                                """))
                .andExpect(status().isForbidden());
    }

    private OrderResponse orderResponse(String orderReference) {
        return new OrderResponse(
                orderReference,
                orderReference,
                "Spice Route",
                "Address",
                "COD",
                "PENDING",
                null,
                null,
                "PLACED",
                null,
                null,
                30,
                "25-35 min",
                220.0,
                null,
                Instant.now(),
                List.of(new OrderItemResponse("Paneer Tikka", "img", 220.0, 1, 220.0))
        );
    }
}
