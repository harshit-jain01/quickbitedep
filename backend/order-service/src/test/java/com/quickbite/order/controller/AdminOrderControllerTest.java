package com.quickbite.order.controller;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
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

@WebMvcTest(controllers = AdminOrderController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class AdminOrderControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private OrderService orderService;

    @Test
    void getAllOrders_shouldReturnOrders_whenRoleIsAdmin() throws Exception {
        when(orderService.getAllOrdersForAdmin()).thenReturn(List.of(orderResponse("OD9001")));

        mockMvc.perform(get("/api/v1/admin/orders")
                        .header("X-Authenticated-Role", "ADMIN"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].orderReference").value("OD9001"));
    }

    @Test
    void getAllOrders_shouldReturnForbidden_whenRoleNotAdmin() throws Exception {
        mockMvc.perform(get("/api/v1/admin/orders")
                        .header("X-Authenticated-Role", "CUSTOMER"))
                .andExpect(status().isForbidden());
    }

    @Test
    void getTotalOrders_shouldReturnCount_whenRoleIsAdmin() throws Exception {
        when(orderService.getTotalOrders()).thenReturn(55L);

        mockMvc.perform(get("/api/v1/admin/orders/count")
                        .header("X-Authenticated-Role", "ADMIN"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalOrders").value(55));
    }

    @Test
    void updateOrderStatus_shouldReturnOrder_whenAdminAndValidRequest() throws Exception {
        when(orderService.updateOrderStatusForAdmin("OD9002", "CONFIRMED")).thenReturn(orderResponse("OD9002"));

        mockMvc.perform(put("/api/v1/admin/orders/OD9002/status")
                        .header("X-Authenticated-Role", "ADMIN")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "status":"CONFIRMED"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.orderReference").value("OD9002"));

        verify(orderService).updateOrderStatusForAdmin("OD9002", "CONFIRMED");
    }

    @Test
    void updateOrderStatus_shouldReturn400_whenValidationFails() throws Exception {
        mockMvc.perform(put("/api/v1/admin/orders/OD9003/status")
                        .header("X-Authenticated-Role", "ADMIN")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "status":""
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").exists());
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
