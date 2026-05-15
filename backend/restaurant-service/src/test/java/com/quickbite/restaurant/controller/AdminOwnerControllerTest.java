package com.quickbite.restaurant.controller;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.quickbite.restaurant.dto.AdminRestaurantOwnerResponse;
import com.quickbite.restaurant.exception.GlobalExceptionHandler;
import com.quickbite.restaurant.security.JwtAuthenticationFilter;
import com.quickbite.restaurant.service.RestaurantOwnerService;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(controllers = AdminOwnerController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class AdminOwnerControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private RestaurantOwnerService restaurantOwnerService;

    @MockBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @Test
    void getOwners_shouldReturn200_whenRoleIsAdmin() throws Exception {
        when(restaurantOwnerService.getAllOwnersForAdmin()).thenReturn(List.of(
                new AdminRestaurantOwnerResponse(
                        1L,
                        UUID.fromString("11111111-1111-1111-1111-111111111111"),
                        "Spice Villa",
                        "Indian",
                        "MP Nagar",
                        "owner@quickbite.com",
                        "9999911111",
                        true,
                        LocalDateTime.now()
                )
        ));

        mockMvc.perform(get("/api/v1/admin/owners").header("X-Authenticated-Role", "ADMIN"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].restaurantName").value("Spice Villa"));

        verify(restaurantOwnerService).getAllOwnersForAdmin();
    }

    @Test
    void getOwners_shouldReturn403_whenRoleIsNotAdmin() throws Exception {
        mockMvc.perform(get("/api/v1/admin/owners").header("X-Authenticated-Role", "CUSTOMER"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("You do not have permission to access this resource"));

        verify(restaurantOwnerService, never()).getAllOwnersForAdmin();
    }
}
