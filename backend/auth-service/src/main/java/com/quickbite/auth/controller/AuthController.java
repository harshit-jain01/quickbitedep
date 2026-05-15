package com.quickbite.auth.controller;

import com.quickbite.auth.dto.AuthResponse;
import com.quickbite.auth.dto.LoginRequest;
import com.quickbite.auth.dto.RegisterRequest;
import com.quickbite.auth.dto.UpdatePasswordRequest;
import com.quickbite.auth.dto.UpdateProfileRequest;
import com.quickbite.auth.dto.UserProfileResponse;
import com.quickbite.auth.dto.ValidateTokenRequest;
import com.quickbite.auth.dto.ValidateTokenResponse;
import com.quickbite.auth.service.AuthService;
import jakarta.validation.Valid;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private static final Logger logger = LoggerFactory.getLogger(AuthController.class);

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public AuthResponse register(@Valid @RequestBody RegisterRequest request) {
        logger.info("Auth register request received for email={}", request.email());
        return authService.register(request);
    }

    @PostMapping("/login")
    public AuthResponse login(@Valid @RequestBody LoginRequest request) {
        logger.info("Auth login request received for email={}", request.email());
        return authService.login(request);
    }

    @GetMapping("/profile")
    public UserProfileResponse getProfile(@AuthenticationPrincipal UserDetails userDetails) {
        logger.debug("Profile fetch requested for email={}", userDetails.getUsername());
        return authService.getProfile(userDetails.getUsername());
    }

    @PutMapping("/profile")
    public UserProfileResponse updateProfile(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody UpdateProfileRequest request
    ) {
        logger.info("Profile update requested for email={}", userDetails.getUsername());
        return authService.updateProfile(userDetails.getUsername(), request);
    }

    @PutMapping("/password")
    public Map<String, String> updatePassword(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody UpdatePasswordRequest request
    ) {
        logger.info("Password update requested for email={}", userDetails.getUsername());
        authService.updatePassword(userDetails.getUsername(), request);
        return Map.of("message", "Password updated successfully");
    }

    @GetMapping("/oauth2/success")
    public Map<String, String> oauth2Success(
            @RequestParam(value = "token", required = false) String tokenParam,
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authHeader
    ) {
        logger.info("OAuth2 success endpoint invoked");
        String token = tokenParam != null && !tokenParam.isBlank()
                ? tokenParam
                : authHeader != null && authHeader.startsWith("Bearer ")
                ? authHeader.substring(7)
                : "";
        return Map.of(
                "message", "OAuth2 login completed successfully",
                "token", token
        );
    }

    @PostMapping("/validate")
    public ValidateTokenResponse validate(
            @Valid @RequestBody ValidateTokenRequest request,
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authHeader
    ) {
        logger.debug("Token validation request received");
        return authService.validateToken(request, authHeader);
    }
}
