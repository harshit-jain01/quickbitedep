package com.quickbite.auth.service.impl;

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
import com.quickbite.auth.service.AuthService;
import com.quickbite.auth.service.UserMapper;
import java.util.Locale;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class AuthServiceImpl implements AuthService {

    private static final Logger logger = LoggerFactory.getLogger(AuthServiceImpl.class);

    private static final int PROFILE_PIC_URL_MAX_LENGTH = 255;

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final UserMapper userMapper;
    private final UserDetailsService userDetailsService;
    private final FixedAdminProperties fixedAdminProperties;

    public AuthServiceImpl(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            AuthenticationManager authenticationManager,
            JwtService jwtService,
            UserMapper userMapper,
            UserDetailsService userDetailsService,
            FixedAdminProperties fixedAdminProperties
    ) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
        this.userMapper = userMapper;
        this.userDetailsService = userDetailsService;
        this.fixedAdminProperties = fixedAdminProperties;
    }

    @Override
    public AuthResponse register(RegisterRequest request) {
        String normalizedEmail = normalizeEmail(request.email());
        logger.info("Registering new user for email={}", normalizedEmail);
        if (userRepository.existsByEmailIgnoreCase(normalizedEmail)) {
            logger.warn("Registration rejected because email already exists: {}", normalizedEmail);
            throw new IllegalArgumentException("Email is already registered");
        }

        UserAccount user = new UserAccount();
        user.setFullName(request.fullName().trim());
        user.setEmail(normalizedEmail);
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setPhone(normalizePhone(request.phone()));
        UserRole requestedRole = request.role() == null ? UserRole.CUSTOMER : request.role();
        user.setRole(requestedRole == UserRole.ADMIN ? UserRole.CUSTOMER : requestedRole);
        user.setProvider(AuthProvider.LOCAL);
        user.setActive(true);
        user.setProfilePicUrl(sanitizeProfilePicUrl(request.profilePicUrl()));

        UserAccount savedUser = userRepository.save(user);
        logger.info("User registered successfully with role={} email={}", savedUser.getRole(), savedUser.getEmail());
        String token = jwtService.generateToken(savedUser);
        return new AuthResponse(token, "Bearer", jwtService.getExpirationSeconds(), userMapper.toProfile(savedUser));
    }

    @Override
    public AuthResponse login(LoginRequest request) {
        String normalizedEmail = normalizeEmail(request.email());
        logger.info("Login attempt for email={}", normalizedEmail);
        if (isFixedAdminCredential(normalizedEmail, request.password())) {
            logger.info("Fixed admin login flow used for email={}", normalizedEmail);
            UserAccount admin = userRepository.findByEmailIgnoreCase(fixedAdminProperties.email())
                    .orElseThrow(() -> new BadCredentialsException("Fixed admin account is not available"));
            String token = jwtService.generateToken(admin);
            return new AuthResponse(token, "Bearer", jwtService.getExpirationSeconds(), userMapper.toProfile(admin));
        }

        userRepository.findByEmailIgnoreCase(normalizedEmail)
                .filter(user -> user.getProvider() != AuthProvider.LOCAL)
                .ifPresent(user -> {
                    throw new BadCredentialsException("This account was created with Google login. Please sign in with Google.");
                });

        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(normalizedEmail, request.password())
        );

        UserAccount user = userRepository.findByEmailIgnoreCase(normalizedEmail)
                .orElseThrow(() -> new BadCredentialsException("Invalid email or password"));
        logger.info("Login successful for email={} role={}", user.getEmail(), user.getRole());
        String token = jwtService.generateToken(user);
        return new AuthResponse(token, "Bearer", jwtService.getExpirationSeconds(), userMapper.toProfile(user));
    }

    @Override
    @Transactional(readOnly = true)
    public UserProfileResponse getProfile(String email) {
        logger.debug("Fetching profile for email={}", email);
        UserAccount user = userRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        return userMapper.toProfile(user);
    }

    @Override
    public UserProfileResponse updateProfile(String email, UpdateProfileRequest request) {
        logger.info("Updating profile for email={}", email);
        UserAccount user = userRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        user.setFullName(request.fullName().trim());
        user.setPhone(normalizePhone(request.phone()));
        user.setProfilePicUrl(sanitizeProfilePicUrl(request.profilePicUrl()));
        return userMapper.toProfile(userRepository.save(user));
    }

    @Override
    public void updatePassword(String email, UpdatePasswordRequest request) {
        logger.info("Updating password for email={}", email);
        UserAccount user = userRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        if (user.getProvider() != AuthProvider.LOCAL) {
            throw new IllegalArgumentException("Password update is not available for Google login accounts");
        }
        if (!passwordEncoder.matches(request.currentPassword(), user.getPasswordHash())) {
            throw new IllegalArgumentException("Current password is incorrect");
        }
        if (request.currentPassword().equals(request.newPassword())) {
            throw new IllegalArgumentException("New password must be different from current password");
        }

        user.setPasswordHash(passwordEncoder.encode(request.newPassword()));
        userRepository.save(user);
    }

    @Override
    @Transactional(readOnly = true)
    public ValidateTokenResponse validateToken(ValidateTokenRequest request, String authHeader) {
        String token = extractToken(request, authHeader);
        logger.debug("Validating token request, tokenPresent={}", token != null && !token.isBlank());
        if (token == null || !jwtService.isTokenValid(token)) {
            logger.warn("Token validation failed: token missing or invalid");
            return new ValidateTokenResponse(false, null, null);
        }
        String email = jwtService.extractSubject(token);
        UserDetails userDetails = userDetailsService.loadUserByUsername(email);
        if (!jwtService.isTokenValid(token, userDetails)) {
            logger.warn("Token validation failed for subject={}", email);
            return new ValidateTokenResponse(false, null, null);
        }
        UserAccount user = userRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        logger.debug("Token validation successful for email={}", user.getEmail());
        return new ValidateTokenResponse(true, user.getEmail(), user.getRole());
    }

    private String extractToken(ValidateTokenRequest request, String authHeader) {
        if (request != null && request.token() != null && !request.token().isBlank()) {
            return request.token();
        }
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            return authHeader.substring(7);
        }
        return null;
    }

    private boolean isFixedAdminCredential(String email, String password) {
        return email.equals(fixedAdminProperties.email().trim().toLowerCase(Locale.ROOT))
                && fixedAdminProperties.password().equals(password);
    }

    private String normalizeEmail(String email) {
        return email == null ? "" : email.trim().toLowerCase(Locale.ROOT);
    }

    private String normalizePhone(String phone) {
        if (phone == null) {
            return null;
        }
        String normalizedPhone = phone.trim();
        return normalizedPhone.isEmpty() ? null : normalizedPhone;
    }

    private String sanitizeProfilePicUrl(String profilePicUrl) {
        if (profilePicUrl == null) {
            return null;
        }
        String normalizedUrl = profilePicUrl.trim();
        if (normalizedUrl.isEmpty()) {
            return null;
        }
        return normalizedUrl.length() <= PROFILE_PIC_URL_MAX_LENGTH
                ? normalizedUrl
                : normalizedUrl.substring(0, PROFILE_PIC_URL_MAX_LENGTH);
    }
}

