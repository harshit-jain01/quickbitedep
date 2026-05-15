package com.quickbite.tracking.controller;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.quickbite.tracking.dto.TrackingResponse;
import com.quickbite.tracking.service.TrackingService;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(controllers = TrackingController.class)
@AutoConfigureMockMvc(addFilters = false)
class TrackingControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private TrackingService trackingService;

    @Test
    void getOrderTracking_shouldReturn200_whenFound() throws Exception {
        when(trackingService.getOrderTracking("user@mail.com", "OD7001")).thenReturn(new TrackingResponse(
                "OD7001",
                "CONFIRMED",
                "Ravi",
                "20-30 min",
                "Restaurant confirmed your order",
                LocalDateTime.now()
        ));

        mockMvc.perform(get("/api/v1/tracking/orders/OD7001").header("X-Authenticated-User", "user@mail.com"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.orderReference").value("OD7001"));

        verify(trackingService).getOrderTracking(eq("user@mail.com"), eq("OD7001"));
    }

    @Test
    void health_shouldReturn200() throws Exception {
        mockMvc.perform(get("/api/v1/tracking/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").value("Tracking Service is running"));
    }
}

