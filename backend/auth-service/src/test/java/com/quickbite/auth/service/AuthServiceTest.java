package com.quickbite.auth.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.quickbite.auth.config.FixedAdminProperties;
import com.quickbite.auth.dto.AuthResponse;
import com.quickbite.auth.dto.LoginRequest;
import com.quickbite.auth.dto.RegisterRequest;
import com.quickbite.auth.dto.UpdatePasswordRequest;
import com.quickbite.auth.dto.UpdateProfileRequest;
import com.quickbite.auth.dto.UserProfileResponse;
import com.quickbite.auth.dto.ValidateTokenRequest;
import com.quickbite.auth.dto.ValidateTokenResponse;
import com.quickbite.auth.model.AuthProvider;
import com.quickbite.auth.model.UserAccount;
import com.quickbite.auth.model.UserRole;
import com.quickbite.auth.repository.UserRepository;
import com.quickbite.auth.security.JwtService;
import com.quickbite.auth.service.impl.AuthServiceImpl;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private JwtService jwtService;

    @Mock
    private UserMapper userMapper;

    @Mock
    private UserDetailsService userDetailsService;

    @Mock
    private FixedAdminProperties fixedAdminProperties;

    @InjectMocks
    private AuthServiceImpl authService;

    @Test
    void register_shouldCreateUserAndReturnTokenResponse() {
        RegisterRequest request = new RegisterRequest(
                "  John Doe  ",
                "  JOHN@MAIL.COM ",
                "Password@123",
                " 9999999999 ",
                UserRole.ADMIN,
                "https://img/pic.png"
        );

        UserAccount saved = user("john@mail.com", AuthProvider.LOCAL, UserRole.CUSTOMER);
        when(userRepository.existsByEmailIgnoreCase("john@mail.com")).thenReturn(false);
        when(passwordEncoder.encode("Password@123")).thenReturn("ENC");
        when(userRepository.save(any(UserAccount.class))).thenReturn(saved);
        when(jwtService.generateToken(saved)).thenReturn("jwt-token");
        when(jwtService.getExpirationSeconds()).thenReturn(3600L);
        when(userMapper.toProfile(saved)).thenReturn(profile(saved));

        var response = authService.register(request);

        assertEquals("jwt-token", response.token());
        assertEquals("Bearer", response.tokenType());
        assertEquals("john@mail.com", response.user().email());

        ArgumentCaptor<UserAccount> captor = ArgumentCaptor.forClass(UserAccount.class);
        verify(userRepository).save(captor.capture());
        assertEquals("John Doe", captor.getValue().getFullName());
        assertEquals(UserRole.CUSTOMER, captor.getValue().getRole());
        assertEquals("9999999999", captor.getValue().getPhone());
    }

    @Test
    void register_shouldUseDefaultsAndSanitizeOptionalFields() {
        String longUrl = "https://x".repeat(100);
        RegisterRequest request = new RegisterRequest(
                "  Jane Doe ",
                " JANE@mail.com ",
                "Password@123",
                "   ",
                null,
                longUrl
        );

        UserAccount saved = user("jane@mail.com", AuthProvider.LOCAL, UserRole.CUSTOMER);
        when(userRepository.existsByEmailIgnoreCase("jane@mail.com")).thenReturn(false);
        when(passwordEncoder.encode("Password@123")).thenReturn("ENC_JANE");
        when(userRepository.save(any(UserAccount.class))).thenReturn(saved);
        when(jwtService.generateToken(saved)).thenReturn("token-jane");
        when(jwtService.getExpirationSeconds()).thenReturn(1800L);
        when(userMapper.toProfile(saved)).thenReturn(profile(saved));

        authService.register(request);

        ArgumentCaptor<UserAccount> captor = ArgumentCaptor.forClass(UserAccount.class);
        verify(userRepository).save(captor.capture());
        assertEquals(UserRole.CUSTOMER, captor.getValue().getRole());
        assertNull(captor.getValue().getPhone());
        assertEquals(255, captor.getValue().getProfilePicUrl().length());
    }

    @Test
    void register_shouldThrow_whenEmailAlreadyExists() {
        RegisterRequest request = new RegisterRequest("Aaa", "a@mail.com", "Password@123", null, UserRole.CUSTOMER, null);
        when(userRepository.existsByEmailIgnoreCase("a@mail.com")).thenReturn(true);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> authService.register(request));

        assertEquals("Email is already registered", ex.getMessage());
        verify(userRepository, never()).save(any());
    }

    @Test
    void login_shouldUseFixedAdminFlow_whenCredentialMatches() {
        LoginRequest request = new LoginRequest("admin@mail.com", "admin123");
        UserAccount admin = user("admin@mail.com", AuthProvider.LOCAL, UserRole.ADMIN);

        when(fixedAdminProperties.email()).thenReturn("admin@mail.com");
        when(fixedAdminProperties.password()).thenReturn("admin123");
        when(userRepository.findByEmailIgnoreCase("admin@mail.com")).thenReturn(Optional.of(admin));
        when(jwtService.generateToken(admin)).thenReturn("admin-token");
        when(jwtService.getExpirationSeconds()).thenReturn(7200L);
        when(userMapper.toProfile(admin)).thenReturn(profile(admin));

        var response = authService.login(request);

        assertEquals("admin-token", response.token());
        assertEquals(UserRole.ADMIN, response.user().role());
        verify(authenticationManager, never()).authenticate(any());
    }

    @Test
    void login_shouldThrow_whenFixedAdminUserIsMissing() {
        when(fixedAdminProperties.email()).thenReturn("admin@mail.com");
        when(fixedAdminProperties.password()).thenReturn("admin123");
        when(userRepository.findByEmailIgnoreCase("admin@mail.com")).thenReturn(Optional.empty());

        BadCredentialsException ex = assertThrows(
                BadCredentialsException.class,
                () -> authService.login(new LoginRequest("admin@mail.com", "admin123"))
        );

        assertTrue(ex.getMessage().contains("not available"));
    }

    @Test
    void login_shouldThrow_whenGoogleAccountUsesPasswordLogin() {
        LoginRequest request = new LoginRequest("google@mail.com", "password123");
        UserAccount googleUser = user("google@mail.com", AuthProvider.GOOGLE, UserRole.CUSTOMER);

        when(fixedAdminProperties.email()).thenReturn("admin@mail.com");
        when(userRepository.findByEmailIgnoreCase("google@mail.com")).thenReturn(Optional.of(googleUser));

        BadCredentialsException ex = assertThrows(BadCredentialsException.class, () -> authService.login(request));

        assertTrue(ex.getMessage().contains("Google login"));
        verify(authenticationManager, never()).authenticate(any());
    }

    @Test
    void login_shouldAuthenticateAndReturnResponse_forLocalUser() {
        LoginRequest request = new LoginRequest(" local@mail.com ", "Password@123");
        UserAccount localUser = user("local@mail.com", AuthProvider.LOCAL, UserRole.CUSTOMER);

        when(fixedAdminProperties.email()).thenReturn("admin@mail.com");
        when(userRepository.findByEmailIgnoreCase("local@mail.com")).thenReturn(Optional.of(localUser));
        when(jwtService.generateToken(localUser)).thenReturn("local-token");
        when(jwtService.getExpirationSeconds()).thenReturn(3600L);
        when(userMapper.toProfile(localUser)).thenReturn(profile(localUser));

        AuthResponse response = authService.login(request);

        assertEquals("local-token", response.token());
        verify(authenticationManager, times(1)).authenticate(any());
        verify(userRepository, times(2)).findByEmailIgnoreCase("local@mail.com");
    }

    @Test
    void login_shouldThrow_whenUserMissingAfterAuthentication() {
        LoginRequest request = new LoginRequest("notfound@mail.com", "Password@123");
        when(fixedAdminProperties.email()).thenReturn("admin@mail.com");
        when(userRepository.findByEmailIgnoreCase("notfound@mail.com")).thenReturn(Optional.empty());

        BadCredentialsException ex = assertThrows(BadCredentialsException.class, () -> authService.login(request));

        assertEquals("Invalid email or password", ex.getMessage());
    }

    @Test
    void getProfile_shouldReturnProfile_whenUserExists() {
        UserAccount user = user("user@mail.com", AuthProvider.LOCAL, UserRole.CUSTOMER);
        UserProfileResponse mapped = profile(user);
        when(userRepository.findByEmailIgnoreCase("user@mail.com")).thenReturn(Optional.of(user));
        when(userMapper.toProfile(user)).thenReturn(mapped);

        UserProfileResponse response = authService.getProfile("user@mail.com");

        assertEquals("user@mail.com", response.email());
    }

    @Test
    void getProfile_shouldThrow_whenUserMissing() {
        when(userRepository.findByEmailIgnoreCase("missing@mail.com")).thenReturn(Optional.empty());

        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> authService.getProfile("missing@mail.com")
        );

        assertEquals("User not found", ex.getMessage());
    }

    @Test
    void updateProfile_shouldNormalizeFieldsAndSave() {
        UserAccount user = user("user@mail.com", AuthProvider.LOCAL, UserRole.CUSTOMER);
        UpdateProfileRequest request = new UpdateProfileRequest("  Updated Name ", " ", "https://pic");
        when(userRepository.findByEmailIgnoreCase("user@mail.com")).thenReturn(Optional.of(user));
        when(userRepository.save(any(UserAccount.class))).thenReturn(user);
        when(userMapper.toProfile(user)).thenReturn(profile(user));

        UserProfileResponse response = authService.updateProfile("user@mail.com", request);

        assertEquals("Updated Name", user.getFullName());
        assertNull(user.getPhone());
        assertEquals("https://pic", user.getProfilePicUrl());
        assertEquals("user@mail.com", response.email());
    }

    @Test
    void updatePassword_shouldEncodeAndSave_whenCurrentPasswordMatches() {
        UserAccount user = user("user@mail.com", AuthProvider.LOCAL, UserRole.CUSTOMER);
        user.setPasswordHash("ENC_OLD");
        UpdatePasswordRequest request = new UpdatePasswordRequest("Current@123", "NewPass@123");
        when(userRepository.findByEmailIgnoreCase("user@mail.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("Current@123", "ENC_OLD")).thenReturn(true);
        when(passwordEncoder.encode("NewPass@123")).thenReturn("ENC_NEW");

        authService.updatePassword("user@mail.com", request);

        assertEquals("ENC_NEW", user.getPasswordHash());
        verify(userRepository).save(user);
    }

    @Test
    void updatePassword_shouldThrow_whenCurrentPasswordIsWrong() {
        UserAccount user = user("user@mail.com", AuthProvider.LOCAL, UserRole.CUSTOMER);
        user.setPasswordHash("ENC_OLD");
        when(userRepository.findByEmailIgnoreCase("user@mail.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("Wrong@123", "ENC_OLD")).thenReturn(false);

        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> authService.updatePassword("user@mail.com", new UpdatePasswordRequest("Wrong@123", "NewPass@123"))
        );

        assertEquals("Current password is incorrect", ex.getMessage());
        verify(userRepository, never()).save(any(UserAccount.class));
    }

    @Test
    void validateToken_shouldUseAuthorizationHeader_whenRequestTokenIsBlank() {
        UserAccount user = user("header@mail.com", AuthProvider.LOCAL, UserRole.CUSTOMER);
        var userDetails = User.withUsername("header@mail.com").password("x").authorities("ROLE_CUSTOMER").build();
        when(jwtService.isTokenValid("header-token")).thenReturn(true);
        when(jwtService.extractSubject("header-token")).thenReturn("header@mail.com");
        when(userDetailsService.loadUserByUsername("header@mail.com")).thenReturn(userDetails);
        when(jwtService.isTokenValid("header-token", userDetails)).thenReturn(true);
        when(userRepository.findByEmailIgnoreCase("header@mail.com")).thenReturn(Optional.of(user));

        ValidateTokenResponse response = authService.validateToken(new ValidateTokenRequest(" "), "Bearer header-token");

        assertTrue(response.valid());
        assertEquals("header@mail.com", response.email());
    }

    @Test
    void validateToken_shouldReturnInvalid_whenTokenIsMissingOrInvalid() {
        when(jwtService.isTokenValid("bad-token")).thenReturn(false);

        var response = authService.validateToken(new ValidateTokenRequest("bad-token"), null);

        assertFalse(response.valid());
        assertNull(response.email());
        verify(userDetailsService, never()).loadUserByUsername(any());
    }

    @Test
    void validateToken_shouldReturnInvalid_whenUserDetailsValidationFails() {
        var userDetails = User.withUsername("x@mail.com").password("x").authorities("ROLE_CUSTOMER").build();
        when(jwtService.isTokenValid("valid-token")).thenReturn(true);
        when(jwtService.extractSubject("valid-token")).thenReturn("x@mail.com");
        when(userDetailsService.loadUserByUsername("x@mail.com")).thenReturn(userDetails);
        when(jwtService.isTokenValid("valid-token", userDetails)).thenReturn(false);

        ValidateTokenResponse response = authService.validateToken(new ValidateTokenRequest("valid-token"), null);

        assertFalse(response.valid());
        verify(userRepository, never()).findByEmailIgnoreCase(any());
    }

    @Test
    void validateToken_shouldThrow_whenSubjectUserNotFound() {
        var userDetails = User.withUsername("x@mail.com").password("x").authorities("ROLE_CUSTOMER").build();
        when(jwtService.isTokenValid("valid-token")).thenReturn(true);
        when(jwtService.extractSubject("valid-token")).thenReturn("x@mail.com");
        when(userDetailsService.loadUserByUsername("x@mail.com")).thenReturn(userDetails);
        when(jwtService.isTokenValid("valid-token", userDetails)).thenReturn(true);
        when(userRepository.findByEmailIgnoreCase("x@mail.com")).thenReturn(Optional.empty());

        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> authService.validateToken(new ValidateTokenRequest("valid-token"), null)
        );

        assertEquals("User not found", ex.getMessage());
    }

    @Test
    void validateToken_shouldReturnValidResponse_whenAllChecksPass() {
        UserAccount user = user("ok@mail.com", AuthProvider.LOCAL, UserRole.ADMIN);
        var userDetails = User.withUsername("ok@mail.com").password("x").authorities("ROLE_ADMIN").build();
        when(jwtService.isTokenValid("ok-token")).thenReturn(true);
        when(jwtService.extractSubject("ok-token")).thenReturn("ok@mail.com");
        when(userDetailsService.loadUserByUsername("ok@mail.com")).thenReturn(userDetails);
        when(jwtService.isTokenValid("ok-token", userDetails)).thenReturn(true);
        when(userRepository.findByEmailIgnoreCase("ok@mail.com")).thenReturn(Optional.of(user));

        ValidateTokenResponse response = authService.validateToken(new ValidateTokenRequest("ok-token"), "Bearer ignored");

        assertTrue(response.valid());
        assertEquals("ok@mail.com", response.email());
        assertEquals(UserRole.ADMIN, response.role());
        verify(jwtService, never()).isTokenValid(eq("ignored"));
    }

    private UserAccount user(String email, AuthProvider provider, UserRole role) {
        UserAccount user = new UserAccount();
        user.setUserId(UUID.randomUUID());
        user.setEmail(email);
        user.setFullName("Test User");
        user.setPhone("9999999999");
        user.setProvider(provider);
        user.setRole(role);
        user.setActive(true);
        user.setCreatedAt(Instant.now());
        return user;
    }

    private UserProfileResponse profile(UserAccount user) {
        return new UserProfileResponse(
                user.getUserId(),
                user.getFullName(),
                user.getEmail(),
                user.getPhone(),
                user.getRole(),
                user.getProvider(),
                user.isActive(),
                user.getCreatedAt(),
                user.getProfilePicUrl()
        );
    }
}

