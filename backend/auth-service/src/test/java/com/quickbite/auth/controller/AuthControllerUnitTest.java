package com.quickbite.auth.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.quickbite.auth.dto.AuthResponse;
import com.quickbite.auth.dto.LoginRequest;
import com.quickbite.auth.dto.RegisterRequest;
import com.quickbite.auth.dto.UpdatePasswordRequest;
import com.quickbite.auth.dto.UpdateProfileRequest;
import com.quickbite.auth.dto.UserProfileResponse;
import com.quickbite.auth.dto.ValidateTokenRequest;
import com.quickbite.auth.dto.ValidateTokenResponse;
import com.quickbite.auth.model.AuthProvider;
import com.quickbite.auth.model.UserRole;
import com.quickbite.auth.service.AuthService;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;

@ExtendWith(MockitoExtension.class)
class AuthControllerUnitTest {

    @Mock
    private AuthService authService;

    private AuthController authController;

    @BeforeEach
    void setUp() {
        authController = new AuthController(authService);
    }

    @Test
    void register_shouldReturnAuthResponse_whenValidRequest() {
        RegisterRequest request = new RegisterRequest("John Doe", "john@mail.com", "password123", "1234567890", UserRole.CUSTOMER, null);
        AuthResponse expectedResponse = createAuthResponse("token123", "john@mail.com");
        
        when(authService.register(request)).thenReturn(expectedResponse);

        AuthResponse result = authController.register(request);

        assertNotNull(result);
        assertEquals("token123", result.token());
        assertEquals("john@mail.com", result.user().email());
        verify(authService).register(request);
    }

    @Test
    void login_shouldReturnAuthResponse_whenValidCredentials() {
        LoginRequest request = new LoginRequest("john@mail.com", "password123");
        AuthResponse expectedResponse = createAuthResponse("login-token", "john@mail.com");
        
        when(authService.login(request)).thenReturn(expectedResponse);

        AuthResponse result = authController.login(request);

        assertNotNull(result);
        assertEquals("login-token", result.token());
        verify(authService).login(request);
    }

    @Test
    void getProfile_shouldReturnUserProfile_whenAuthenticated() {
        UserDetails userDetails = User.withUsername("john@mail.com").password("pwd").roles("CUSTOMER").build();
        UserProfileResponse expectedProfile = createUserProfile("john@mail.com");
        
        when(authService.getProfile("john@mail.com")).thenReturn(expectedProfile);

        UserProfileResponse result = authController.getProfile(userDetails);

        assertNotNull(result);
        assertEquals("john@mail.com", result.email());
        verify(authService).getProfile("john@mail.com");
    }

    @Test
    void updateProfile_shouldReturnUpdatedProfile_whenValidRequest() {
        UserDetails userDetails = User.withUsername("john@mail.com").password("pwd").roles("CUSTOMER").build();
        UpdateProfileRequest request = new UpdateProfileRequest("John Updated", "9876543210", "https://newpic.com");
        UserProfileResponse expectedProfile = createUserProfile("john@mail.com");
        
        when(authService.updateProfile(eq("john@mail.com"), any(UpdateProfileRequest.class))).thenReturn(expectedProfile);

        UserProfileResponse result = authController.updateProfile(userDetails, request);

        assertNotNull(result);
        assertEquals("john@mail.com", result.email());
        verify(authService).updateProfile(eq("john@mail.com"), eq(request));
    }

    @Test
    void updatePassword_shouldReturnSuccessMessage_whenValidRequest() {
        UserDetails userDetails = User.withUsername("john@mail.com").password("pwd").roles("CUSTOMER").build();
        UpdatePasswordRequest request = new UpdatePasswordRequest("Current@123", "NewPass@123");

        Map<String, String> result = authController.updatePassword(userDetails, request);

        assertNotNull(result);
        assertEquals("Password updated successfully", result.get("message"));
        verify(authService).updatePassword(eq("john@mail.com"), eq(request));
    }

    @Test
    void validate_shouldReturnValidationResponse_whenValidToken() {
        ValidateTokenRequest request = new ValidateTokenRequest("valid-token");
        ValidateTokenResponse expectedResponse = new ValidateTokenResponse(true, "john@mail.com", UserRole.CUSTOMER);
        
        when(authService.validateToken(request, "Bearer valid-token")).thenReturn(expectedResponse);

        ValidateTokenResponse result = authController.validate(request, "Bearer valid-token");

        assertNotNull(result);
        assertEquals(true, result.valid());
        assertEquals("john@mail.com", result.email());
        verify(authService).validateToken(request, "Bearer valid-token");
    }

    @Test
    void oauth2Success_shouldReturnTokenFromQueryParam() {
        Map<String, String> result = authController.oauth2Success("query-token", "Bearer header-token");

        assertNotNull(result);
        assertEquals("query-token", result.get("token"));
        assertEquals("OAuth2 login completed successfully", result.get("message"));
    }

    @Test
    void oauth2Success_shouldReturnTokenFromHeader_whenNoQueryParam() {
        Map<String, String> result = authController.oauth2Success(null, "Bearer header-token");

        assertNotNull(result);
        assertEquals("header-token", result.get("token"));
    }

    @Test
    void oauth2Success_shouldReturnEmptyToken_whenNoTokenProvided() {
        Map<String, String> result = authController.oauth2Success(null, null);

        assertNotNull(result);
        assertEquals("", result.get("token"));
    }

    private AuthResponse createAuthResponse(String token, String email) {
        return new AuthResponse(
                token,
                "Bearer",
                3600,
                createUserProfile(email)
        );
    }

    private UserProfileResponse createUserProfile(String email) {
        return new UserProfileResponse(
                UUID.randomUUID(),
                "John Doe",
                email,
                "1234567890",
                UserRole.CUSTOMER,
                AuthProvider.LOCAL,
                true,
                Instant.now(),
                null
        );
    }
}
