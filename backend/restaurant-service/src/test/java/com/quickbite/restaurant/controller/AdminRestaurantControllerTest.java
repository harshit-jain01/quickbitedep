package com.quickbite.restaurant.controller;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.quickbite.restaurant.dto.AdminRestaurantOwnerResponse;
import com.quickbite.restaurant.dto.RestaurantResponse;
import com.quickbite.restaurant.exception.GlobalExceptionHandler;
import com.quickbite.restaurant.security.JwtAuthenticationFilter;
import com.quickbite.restaurant.service.RestaurantOwnerService;
import com.quickbite.restaurant.service.RestaurantService;
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

@WebMvcTest(controllers = AdminRestaurantController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class AdminRestaurantControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private RestaurantService restaurantService;

    @MockBean
    private RestaurantOwnerService restaurantOwnerService;

    @MockBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @Test
    void getRestaurants_shouldReturn200_whenRoleIsAdmin() throws Exception {
        when(restaurantService.getAllRestaurantsForAdmin()).thenReturn(List.of(
                new RestaurantResponse(7L, "Burger Hub", "Fast Food", "Arera", List.of("Burger"), 4.2, 30, 400, 1.2, "img", "Best burgers")
        ));

        mockMvc.perform(get("/api/v1/admin/restaurants").header("X-Authenticated-Role", "ADMIN"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Burger Hub"));

        verify(restaurantService).getAllRestaurantsForAdmin();
    }

    @Test
    void getOwners_shouldReturn200_whenRoleIsAdmin() throws Exception {
        when(restaurantOwnerService.getAllOwnersForAdmin()).thenReturn(List.of(
                new AdminRestaurantOwnerResponse(
                        5L,
                        UUID.fromString("22222222-2222-2222-2222-222222222222"),
                        "Urban Tadka",
                        "North Indian",
                        "Bittan Market",
                        "owner2@quickbite.com",
                        "9999922222",
                        true,
                        LocalDateTime.now()
                )
        ));

        mockMvc.perform(get("/api/v1/admin/restaurants/owners").header("X-Authenticated-Role", "ADMIN"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].restaurantName").value("Urban Tadka"));

        verify(restaurantOwnerService).getAllOwnersForAdmin();
    }

    @Test
    void deleteRestaurant_shouldReturn204_whenRoleIsAdmin() throws Exception {
        mockMvc.perform(delete("/api/v1/admin/restaurants/12").header("X-Authenticated-Role", "ADMIN"))
                .andExpect(status().isNoContent());

        verify(restaurantService).deleteRestaurantById(12L);
    }

    @Test
    void deleteRestaurant_shouldReturn403_whenRoleIsNotAdmin() throws Exception {
        mockMvc.perform(delete("/api/v1/admin/restaurants/12").header("X-Authenticated-Role", "CUSTOMER"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("You do not have permission to access this resource"));

        verify(restaurantService, never()).deleteRestaurantById(eq(12L));
    }
}
