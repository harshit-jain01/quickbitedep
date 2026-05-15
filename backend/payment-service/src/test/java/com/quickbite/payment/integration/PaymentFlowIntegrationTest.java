package com.quickbite.payment.integration;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.quickbite.payment.repository.PaymentTransactionRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(properties = {
        "spring.cloud.discovery.enabled=false",
        "eureka.client.enabled=false"
})
@AutoConfigureMockMvc
class PaymentFlowIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private PaymentTransactionRepository paymentTransactionRepository;

    @AfterEach
    void cleanUp() {
        paymentTransactionRepository.deleteAll();
    }

    @Test
    void chargeAndFetchStatus_shouldWorkAcrossControllerServiceAndRepository() throws Exception {
        mockMvc.perform(post("/api/v1/payments/charge")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "orderReference":"OD-INT-1",
                                  "customerEmail":"integration@mail.com",
                                  "paymentMethod":"COD",
                                  "amount":550.0
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUCCESS"));

        mockMvc.perform(get("/api/v1/payments/orders/OD-INT-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.orderReference").value("OD-INT-1"))
                .andExpect(jsonPath("$.paymentStatus").value("SUCCESS"));
    }
}
