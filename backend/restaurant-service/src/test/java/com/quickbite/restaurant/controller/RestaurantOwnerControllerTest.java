package com.quickbite.restaurant.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.quickbite.restaurant.client.OrderServiceClient;
import com.quickbite.restaurant.client.ReviewServiceClient;
import com.quickbite.restaurant.dto.RestaurantOwnerProfileResponse;
import com.quickbite.restaurant.exception.GlobalExceptionHandler;
import com.quickbite.restaurant.security.JwtAuthenticationFilter;
import com.quickbite.restaurant.security.JwtService;
import com.quickbite.restaurant.service.RestaurantOwnerService;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(controllers = RestaurantOwnerController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class RestaurantOwnerControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private RestaurantOwnerService restaurantOwnerService;

    @MockBean
    private JwtService jwtService;

    @MockBean
    private OrderServiceClient orderServiceClient;

        @MockBean
        private ReviewServiceClient reviewServiceClient;

    @MockBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @Test
    void getProfile_shouldReturn200_whenTokenContainsUserId() throws Exception {
        UUID ownerId = UUID.fromString("33333333-3333-3333-3333-333333333333");
        when(jwtService.extractUserId("valid-token")).thenReturn(ownerId.toString());
        when(restaurantOwnerService.getRestaurantOwnerProfile(ownerId)).thenReturn(profile(ownerId));

        mockMvc.perform(get("/api/v1/restaurant-owner/profile")
                        .header("Authorization", "Bearer valid-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.restaurantName").value("Spice Route"))
                .andExpect(jsonPath("$.ownerId").value(ownerId.toString()));
    }

    @Test
    void getProfile_shouldReturn400_whenTokenDoesNotContainUserId() throws Exception {
        when(jwtService.extractUserId("bad-token")).thenReturn(null);

        mockMvc.perform(get("/api/v1/restaurant-owner/profile")
                        .header("Authorization", "Bearer bad-token"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Unable to extract user ID from token"));
    }

    @Test
    void register_shouldReturn201_whenPayloadIsValid() throws Exception {
        UUID ownerId = UUID.fromString("44444444-4444-4444-4444-444444444444");
        when(jwtService.extractUserId("register-token")).thenReturn(ownerId.toString());
        when(restaurantOwnerService.getRestaurantOwnerProfile(ownerId)).thenReturn(profile(ownerId));

        MockMultipartFile imageFile = new MockMultipartFile("imageFile", "restaurant.jpg", "image/jpeg", "img".getBytes());
        var authentication = new UsernamePasswordAuthenticationToken(
                User.withUsername("owner@quickbite.com").password("secret").authorities("ROLE_RESTAURANT_OWNER").build(),
                null
        );
        SecurityContextHolder.getContext().setAuthentication(authentication);

        try {
            mockMvc.perform(multipart("/api/v1/restaurant-owner/register")
                            .file(imageFile)
                            .param("restaurantName", "Spice Route")
                            .param("cuisineType", "Indian")
                            .param("address", "MP Nagar")
                            .header("Authorization", "Bearer register-token"))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.restaurantName").value("Spice Route"));
        } finally {
            SecurityContextHolder.clearContext();
        }

        verify(restaurantOwnerService).createRestaurantOwner(eq(ownerId), eq("Spice Route"), eq("Indian"), eq("MP Nagar"), any());
    }

    @Test
    void register_shouldReturn400_whenUserNotAuthenticated() throws Exception {
        MockMultipartFile imageFile = new MockMultipartFile("imageFile", "restaurant.jpg", "image/jpeg", "img".getBytes());

        mockMvc.perform(multipart("/api/v1/restaurant-owner/register")
                        .file(imageFile)
                        .param("restaurantName", "Spice Route")
                        .param("cuisineType", "Indian")
                        .param("address", "MP Nagar")
                        .header("Authorization", "Bearer register-token"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("User not authenticated"));

        verify(restaurantOwnerService, never()).createRestaurantOwner(any(), any(), any(), any(), any());
    }

    @Test
    void getOrders_shouldReturnOnlyOrdersForOwnerRestaurant() throws Exception {
        UUID ownerId = UUID.fromString("55555555-5555-5555-5555-555555555555");
        when(jwtService.extractUserId("orders-token")).thenReturn(ownerId.toString());
        when(restaurantOwnerService.getRestaurantOwnerProfile(ownerId)).thenReturn(profile(ownerId));
        when(orderServiceClient.getOrders(any(), eq("DELIVERY_AGENT"))).thenReturn(List.of(
                Map.of("orderReference", "OD-1", "restaurantName", "Spice Route"),
                Map.of("orderReference", "OD-2", "restaurantName", "Other Kitchen")
        ));

        mockMvc.perform(get("/api/v1/restaurant-owner/orders")
                        .header("Authorization", "Bearer orders-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].orderReference").value("OD-1"))
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    void updateOrderStatus_shouldReturn400_whenDeliveryStatusMissing() throws Exception {
        UUID ownerId = UUID.fromString("66666666-6666-6666-6666-666666666666");
        when(jwtService.extractUserId("status-token")).thenReturn(ownerId.toString());
        when(restaurantOwnerService.getRestaurantOwnerProfile(ownerId)).thenReturn(profile(ownerId));

        mockMvc.perform(patch("/api/v1/restaurant-owner/orders/OD-77")
                        .header("Authorization", "Bearer status-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("deliveryStatus is required"));
    }

    @Test
    void updateOrderStatus_shouldReturn200_andUppercaseDeliveryStatus() throws Exception {
        UUID ownerId = UUID.fromString("77777777-7777-7777-7777-777777777777");
        when(jwtService.extractUserId("status-ok-token")).thenReturn(ownerId.toString());
        when(restaurantOwnerService.getRestaurantOwnerProfile(ownerId)).thenReturn(profile(ownerId));
        when(orderServiceClient.updateOrderStatus(eq("SYSTEM"), eq("OD-88"), anyMap())).thenReturn(
                Map.of("orderReference", "OD-88", "deliveryStatus", "DELIVERED")
        );

        mockMvc.perform(patch("/api/v1/restaurant-owner/orders/OD-88")
                        .header("Authorization", "Bearer status-ok-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "deliveryStatus": "delivered"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.deliveryStatus").value("DELIVERED"));

        verify(orderServiceClient).updateOrderStatus(eq("SYSTEM"), eq("OD-88"), eq(Map.of("deliveryStatus", "DELIVERED")));
    }

    @Test
    void updateProfile_shouldReturn200_whenPayloadIsValid() throws Exception {
        UUID ownerId = UUID.fromString("88888888-8888-8888-8888-888888888888");
        RestaurantOwnerProfileResponse updatePayload = new RestaurantOwnerProfileResponse(
                101L, ownerId, "New Name", "Indian", "Arera", "Desc", "img", 299.0, 30, true
        );
        when(jwtService.extractUserId("update-token")).thenReturn(ownerId.toString());
        when(restaurantOwnerService.updateRestaurantProfile(eq(ownerId), any())).thenReturn(updatePayload);

        mockMvc.perform(put("/api/v1/restaurant-owner/profile")
                        .header("Authorization", "Bearer update-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "restaurantId": 101,
                                  "ownerId": "88888888-8888-8888-8888-888888888888",
                                  "restaurantName": "New Name",
                                  "cuisineType": "Indian",
                                  "area": "Arera",
                                  "description": "Desc",
                                  "imageUrl": "img",
                                  "minimumOrderAmount": 299.0,
                                  "estimatedDeliveryTime": 30,
                                  "isActive": true
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.restaurantName").value("New Name"));
    }

    private RestaurantOwnerProfileResponse profile(UUID ownerId) {
        return new RestaurantOwnerProfileResponse(
                101L,
                ownerId,
                "Spice Route",
                "Indian",
                "MP Nagar",
                "Family dining",
                "img-url",
                199.0,
                35,
                true
        );
    }
}
