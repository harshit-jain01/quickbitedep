package com.quickbite.auth.controller;

import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.quickbite.auth.dto.AdminDashboardResponse;
import com.quickbite.auth.dto.AdminUserResponse;
import com.quickbite.auth.exception.GlobalExceptionHandler;
import com.quickbite.auth.model.UserRole;
import com.quickbite.auth.security.JwtAuthenticationFilter;
import com.quickbite.auth.service.AdminService;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(controllers = AdminController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class AdminControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AdminService adminService;

    @MockBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @Test
    void getDashboard_shouldReturnMetrics() throws Exception {
        when(adminService.getDashboard()).thenReturn(new AdminDashboardResponse(15L, 40L));

        mockMvc.perform(get("/api/v1/admin/dashboard"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalUsers").value(15))
                .andExpect(jsonPath("$.totalOrders").value(40));
    }

    @Test
    void getUsers_shouldReturnUserList() throws Exception {
        UUID userId = UUID.randomUUID();
        when(adminService.getAllUsers()).thenReturn(List.of(
                new AdminUserResponse(userId, "Admin User", "admin@mail.com", "9999999999", UserRole.ADMIN, true, Instant.now())
        ));

        mockMvc.perform(get("/api/v1/admin/users"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].userId").value(userId.toString()))
                .andExpect(jsonPath("$[0].email").value("admin@mail.com"));
    }

    @Test
    void deleteUser_shouldReturnNoContent() throws Exception {
        UUID userId = UUID.randomUUID();
        doNothing().when(adminService).deleteUser(userId);

        mockMvc.perform(delete("/api/v1/admin/users/{id}", userId))
                .andExpect(status().isNoContent());

        verify(adminService).deleteUser(userId);
    }
}
