package com.quickbite.delivery.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.quickbite.delivery.dto.DeliveryAgentResponse;
import com.quickbite.delivery.dto.DeliveryAssignmentResponse;
import com.quickbite.delivery.exception.GlobalExceptionHandler;
import com.quickbite.delivery.service.DeliveryService;
import java.util.LinkedHashMap;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(controllers = DeliveryController.class)
@Import(GlobalExceptionHandler.class)
class DeliveryControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private DeliveryService deliveryService;

    @Test
    void register_shouldReturn201_whenRequestIsValid() throws Exception {
        var request = new LinkedHashMap<String, Object>();
        request.put("name", "Akash");
        request.put("phone", "9999988888");
        request.put("vehicleType", "Bike");
        request.put("vehicleNumber", "KA01AB1234");

        when(deliveryService.register(any())).thenReturn(new DeliveryAgentResponse(
                "AGT1001", "Akash", "9999988888", "Bike", "KA01AB1234", true, true, true, 0
        ));

        mockMvc.perform(post("/api/v1/agents")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value("AGT1001"));

        verify(deliveryService).register(any());
    }

    @Test
    void getAllAgents_shouldReturn200_whenRoleAllowed() throws Exception {
        when(deliveryService.getAllAgents()).thenReturn(List.of(
                new DeliveryAgentResponse("AGT1002", "Ravi", "8888877777", "Bike", "MH01AA1111", true, true, true, 2)
        ));

        mockMvc.perform(get("/api/v1/agents").header("X-Authenticated-Role", "AGENT"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value("AGT1002"));

        verify(deliveryService).getAllAgents();
    }

    @Test
    void getAllAgents_shouldReturn403_whenRoleForbidden() throws Exception {
        mockMvc.perform(get("/api/v1/agents").header("X-Authenticated-Role", "CUSTOMER"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("Forbidden"));

        verify(deliveryService, never()).getAllAgents();
    }

    @Test
    void assign_shouldReturn200_whenValidRole() throws Exception {
        var request = new LinkedHashMap<String, Object>();
        request.put("orderReference", "OD123");
        request.put("restaurantName", "Burger Hub");
        request.put("deliveryAddress", "Main Street");

        when(deliveryService.assign(any())).thenReturn(new DeliveryAssignmentResponse("OD123", null, null, "PENDING_ASSIGNMENT", 0));

        mockMvc.perform(post("/api/v1/agents/assign")
                        .header("X-Authenticated-Role", "SYSTEM")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.orderReference").value("OD123"));
    }
}

