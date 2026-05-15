package com.quickbite.cart.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.quickbite.cart.dto.CartResponse;
import com.quickbite.cart.exception.GlobalExceptionHandler;
import com.quickbite.cart.security.JwtAuthenticationFilter;
import com.quickbite.cart.service.CartService;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(controllers = CartController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class CartControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private CartService cartService;

    @MockBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @Test
    void getCart_shouldReturn200_whenUserHeaderPresent() throws Exception {
        when(cartService.getCart("user@mail.com")).thenReturn(new CartResponse("user@mail.com", null, null, List.of(), 0, 0, 0, 0, 0));

        mockMvc.perform(get("/api/v1/cart").header("X-Authenticated-User", "user@mail.com"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userEmail").value("user@mail.com"));

        verify(cartService).getCart("user@mail.com");
    }

    @Test
    void addItem_shouldReturn201_whenRequestIsValid() throws Exception {
        when(cartService.addItem(eq("user@mail.com"), any())).thenReturn(new CartResponse("user@mail.com", 1L, "R1", List.of(), 1, 100, 0, 0, 100));

        mockMvc.perform(post("/api/v1/cart/items")
                        .header("X-Authenticated-User", "user@mail.com")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "restaurantId":1,
                                  "restaurantName":"R1",
                                  "menuItemId":11,
                                  "itemName":"Burger",
                                  "imageUrl":"img",
                                  "unitPrice":100,
                                  "quantity":1,
                                  "replaceCart":false
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.itemCount").value(1));

        verify(cartService).addItem(eq("user@mail.com"), any());
    }

    @Test
    void addItem_shouldReturn400_whenValidationFails() throws Exception {
        mockMvc.perform(post("/api/v1/cart/items")
                        .header("X-Authenticated-User", "user@mail.com")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "restaurantId":1,
                                  "restaurantName":"R1",
                                  "menuItemId":11,
                                  "itemName":"Burger",
                                  "imageUrl":"img",
                                  "unitPrice":100,
                                  "quantity":0,
                                  "replaceCart":false
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").exists());

        verify(cartService, never()).addItem(eq("user@mail.com"), any());
    }

    @Test
    void clearCart_shouldReturn204() throws Exception {
        mockMvc.perform(delete("/api/v1/cart").header("X-Authenticated-User", "user@mail.com"))
                .andExpect(status().isNoContent());

        verify(cartService).clear("user@mail.com");
    }
}

