package com.quickbite.auth.service;

import com.quickbite.auth.dto.AuthResponse;
import com.quickbite.auth.dto.LoginRequest;
import com.quickbite.auth.dto.RegisterRequest;
import com.quickbite.auth.dto.UpdatePasswordRequest;
import com.quickbite.auth.dto.UpdateProfileRequest;
import com.quickbite.auth.dto.UserProfileResponse;
import com.quickbite.auth.dto.ValidateTokenRequest;
import com.quickbite.auth.dto.ValidateTokenResponse;
public interface AuthService {

    AuthResponse register(RegisterRequest request);

    AuthResponse login(LoginRequest request);

    UserProfileResponse getProfile(String email);

    UserProfileResponse updateProfile(String email, UpdateProfileRequest request);

    void updatePassword(String email, UpdatePasswordRequest request);

    ValidateTokenResponse validateToken(ValidateTokenRequest request, String authHeader);
}
