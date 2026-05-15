package com.quickbite.restaurant.controller;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.quickbite.restaurant.dto.CategoryResponse;
import com.quickbite.restaurant.dto.RestaurantResponse;
import com.quickbite.restaurant.exception.GlobalExceptionHandler;
import com.quickbite.restaurant.security.JwtAuthenticationFilter;
import com.quickbite.restaurant.service.RestaurantService;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(controllers = RestaurantController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class RestaurantControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private RestaurantService restaurantService;

    @MockBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @Test
    void getCategories_shouldReturn200() throws Exception {
        when(restaurantService.getCategories()).thenReturn(List.of(new CategoryResponse("pizza", "Pizza", "img")));

        mockMvc.perform(get("/api/v1/restaurants/categories"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Pizza"));

        verify(restaurantService).getCategories();
    }

    @Test
    void getRestaurants_shouldReturn200WithFilters() throws Exception {
        when(restaurantService.getRestaurants("Pizza", "stone")).thenReturn(List.of(
                new RestaurantResponse(1L, "Firestone Pizza", "Pizza", "Shahpura", List.of("Pizza"), 4.5, 25, 500, 2.0, "img", "desc")
        ));

        mockMvc.perform(get("/api/v1/restaurants").param("category", "Pizza").param("search", "stone"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Firestone Pizza"));

        verify(restaurantService).getRestaurants("Pizza", "stone");
    }

    @Test
    void getRestaurant_shouldReturn400_whenServiceThrows() throws Exception {
        when(restaurantService.getRestaurant(eq(999L))).thenThrow(new IllegalArgumentException("Restaurant not found"));

        mockMvc.perform(get("/api/v1/restaurants/999"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Restaurant not found"));
    }
}

