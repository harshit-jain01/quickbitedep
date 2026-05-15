package com.quickbite.menu.controller;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.quickbite.menu.dto.MenuGroupResponse;
import com.quickbite.menu.dto.MenuItemResponse;
import com.quickbite.menu.exception.GlobalExceptionHandler;
import com.quickbite.menu.service.MenuService;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(controllers = MenuController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class MenuControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private MenuService menuService;

    @Test
    void getRestaurantMenu_shouldReturn200_whenRestaurantHasItems() throws Exception {
        when(menuService.getRestaurantMenu(10L)).thenReturn(List.of(
                new MenuGroupResponse(
                        "Main Course",
                        List.of(new MenuItemResponse(1L, 10L, "Paneer Butter Masala", "Rich gravy", "Main Course", 249.0, true, true, "img"))
                )
        ));

        mockMvc.perform(get("/api/v1/menu/restaurants/10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].category").value("Main Course"))
                .andExpect(jsonPath("$[0].items[0].name").value("Paneer Butter Masala"))
                .andExpect(jsonPath("$[0].items[0].price").value(249.0));

        verify(menuService).getRestaurantMenu(10L);
    }

    @Test
    void getRestaurantMenu_shouldReturn200_whenRestaurantHasNoItems() throws Exception {
        when(menuService.getRestaurantMenu(99L)).thenReturn(List.of());

        mockMvc.perform(get("/api/v1/menu/restaurants/99"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$").isEmpty());

        verify(menuService).getRestaurantMenu(99L);
    }

    @Test
    void getRestaurantMenu_shouldReturn400_whenServiceThrowsIllegalArgument() throws Exception {
        when(menuService.getRestaurantMenu(eq(-1L))).thenThrow(new IllegalArgumentException("Invalid restaurant ID"));

        mockMvc.perform(get("/api/v1/menu/restaurants/-1"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Invalid restaurant ID"));
    }

    @Test
    void getRestaurantMenu_shouldReturn500_whenServiceThrowsRuntimeException() throws Exception {
        when(menuService.getRestaurantMenu(eq(500L))).thenThrow(new RuntimeException("Menu store unavailable"));

        mockMvc.perform(get("/api/v1/menu/restaurants/500"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.message").value("Menu store unavailable"));
    }
}
