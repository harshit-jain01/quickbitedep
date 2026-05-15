package com.quickbite.menu.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.quickbite.menu.dto.MenuItemWithAvailabilityResponse;
import com.quickbite.menu.exception.GlobalExceptionHandler;
import com.quickbite.menu.service.MenuOwnerService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(controllers = MenuOwnerController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class MenuOwnerControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private MenuOwnerService menuOwnerService;

    @Test
    void addMenuItem_shouldReturn201_whenMultipartPayloadValid() throws Exception {
        when(menuOwnerService.addMenuItem(any(), any(), any())).thenReturn(response());

        MockMultipartFile image = new MockMultipartFile("imageFile", "item.jpg", "image/jpeg", "img".getBytes());

        mockMvc.perform(multipart("/api/v1/menu/items")
                        .file(image)
                        .param("restaurantId", "1")
                        .param("itemName", "Paneer Tikka")
                        .param("price", "220.0")
                        .param("description", "Smoky")
                        .param("isAvailable", "true"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Paneer Tikka"));

        verify(menuOwnerService).addMenuItem(any(), any(), any());
    }

    @Test
    void getCategories_shouldReturn400_whenRestaurantIdInvalid() throws Exception {
        mockMvc.perform(get("/api/v1/menu/restaurants/0/categories"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").exists());
    }

    @Test
    void toggleAvailability_shouldReturn200_whenRequestValid() throws Exception {
        when(menuOwnerService.toggleAvailability(eq(5L), any(), eq(false))).thenReturn(response());

        mockMvc.perform(patch("/api/v1/menu/items/5/availability")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "isAvailable": false
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1));

        verify(menuOwnerService).toggleAvailability(eq(5L), any(), eq(false));
    }

    private MenuItemWithAvailabilityResponse response() {
        return new MenuItemWithAvailabilityResponse(1L, 1L, "Paneer Tikka", "Smoky", "Category", 220.0, true, false, "img", true, 1, 1);
    }
}

