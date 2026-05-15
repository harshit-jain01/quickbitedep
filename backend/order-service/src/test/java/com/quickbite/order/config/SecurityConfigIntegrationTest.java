package com.quickbite.order.config;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.quickbite.order.dto.OrderResponse;
import com.quickbite.order.service.OrderService;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(properties = {
        "spring.cloud.discovery.enabled=false",
        "eureka.client.enabled=false"
})
@AutoConfigureMockMvc
class SecurityConfigIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private OrderService orderService;

    @Test
    void orderEndpoints_shouldBeAccessibleWithoutSpringSecurityAuth_whenHeadersPresent() throws Exception {
        when(orderService.getOrders(anyString(), anyString())).thenReturn(List.<OrderResponse>of());

        mockMvc.perform(get("/api/v1/orders")
                        .header("X-Authenticated-User", "user@mail.com")
                        .header("X-Authenticated-Role", "CUSTOMER"))
                .andExpect(status().isOk());
    }

    @Test
    void adminController_shouldStillEnforceRoleCheck() throws Exception {
        mockMvc.perform(get("/api/v1/admin/orders")
                        .header("X-Authenticated-Role", "CUSTOMER"))
                .andExpect(status().isForbidden());
    }
}
