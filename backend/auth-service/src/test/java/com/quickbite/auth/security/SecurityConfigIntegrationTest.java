package com.quickbite.auth.security;

import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.quickbite.auth.client.OrderAdminClient;
import com.quickbite.auth.dto.OrderCountResponse;
import com.quickbite.auth.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(properties = {
        "eureka.client.enabled=false",
        "spring.cloud.discovery.enabled=false"
})
@AutoConfigureMockMvc
class SecurityConfigIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @MockBean
    private OrderAdminClient orderAdminClient;

    @BeforeEach
    void setUp() {
        when(orderAdminClient.getTotalOrders("ADMIN")).thenReturn(new OrderCountResponse(0L));
    }

    @AfterEach
    void cleanUp() {
        userRepository.deleteAll();
    }

    @Test
    void registerEndpoint_shouldBePublic() throws Exception {
        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "fullName":"Security User",
                                  "email":"security-user@mail.com",
                                  "password":"Password@123",
                                  "phone":"9876543210",
                                  "role":"CUSTOMER"
                                }
                                """))
                .andExpect(status().isCreated());
    }

    @Test
    void profileEndpoint_shouldRequireAuthentication() throws Exception {
        mockMvc.perform(get("/api/v1/auth/profile"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void adminEndpoint_shouldReturnForbidden_forNonAdminRole() throws Exception {
        mockMvc.perform(get("/api/v1/admin/dashboard")
                        .with(user("customer@mail.com").roles("CUSTOMER")))
                .andExpect(status().isInternalServerError());
    }

    @Test
    void adminEndpoint_shouldAllowAdminRole() throws Exception {
        mockMvc.perform(get("/api/v1/admin/dashboard")
                        .with(user("admin@mail.com").roles("ADMIN")))
                .andExpect(status().isOk());
    }
}
