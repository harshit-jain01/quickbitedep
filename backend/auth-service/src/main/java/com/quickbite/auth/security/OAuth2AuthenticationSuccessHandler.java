package com.quickbite.auth.security;

import com.quickbite.auth.dto.AuthResponse;
import com.quickbite.auth.model.AuthProvider;
import com.quickbite.auth.model.UserAccount;
import com.quickbite.auth.model.UserRole;
import com.quickbite.auth.repository.UserRepository;
import com.quickbite.auth.service.UserMapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Locale;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

@Component
public class OAuth2AuthenticationSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    private static final int PROFILE_PIC_URL_MAX_LENGTH = 255;

    private final UserRepository userRepository;
    private final JwtService jwtService;
    private final UserMapper userMapper;
    private final ObjectMapper objectMapper;
    private final String redirectUri;

    public OAuth2AuthenticationSuccessHandler(
            UserRepository userRepository,
            JwtService jwtService,
            UserMapper userMapper,
            ObjectMapper objectMapper,
            @Value("${app.oauth2.authorized-redirect-uri}") String redirectUri
    ) {
        this.userRepository = userRepository;
        this.jwtService = jwtService;
        this.userMapper = userMapper;
        this.objectMapper = objectMapper;
        this.redirectUri = redirectUri;
    }

    @Override
    public void onAuthenticationSuccess(
            HttpServletRequest request,
            HttpServletResponse response,
            Authentication authentication
    ) throws IOException, ServletException {
        OAuth2User oAuth2User = (OAuth2User) authentication.getPrincipal();
        String email = normalizeEmail(oAuth2User.getAttribute("email"));
        String name = oAuth2User.getAttribute("name");
        String providerId = oAuth2User.getName();
        String profilePicUrl = sanitizeProfilePicUrl(oAuth2User.getAttribute("picture"));

        if (email.isBlank()) {
            throw new ServletException("Google account email is not available");
        }

        UserAccount user = userRepository.findByEmailIgnoreCase(email)
                .map(existing -> updateOAuthDetails(existing, name, providerId, profilePicUrl))
                .orElseGet(() -> createOAuthUser(email, name, providerId, profilePicUrl));

        userRepository.save(user);
        String token = jwtService.generateToken(user);

        if (redirectUri != null && !redirectUri.isBlank()) {
            getRedirectStrategy().sendRedirect(request, response, redirectUri + "?token=" + token);
            return;
        }

        response.setContentType("application/json");
        response.getWriter().write(objectMapper.writeValueAsString(new AuthResponse(
                token,
                "Bearer",
                jwtService.getExpirationSeconds(),
                userMapper.toProfile(user)
        )));
        clearAuthenticationAttributes(request);
    }

    private UserAccount updateOAuthDetails(UserAccount user, String name, String providerId, String profilePicUrl) {
        user.setFullName(normalizeName(name, user.getFullName()));
        user.setProvider(AuthProvider.GOOGLE);
        user.setProviderId(providerId);
        user.setActive(true);
        if (profilePicUrl != null) {
            user.setProfilePicUrl(profilePicUrl);
        }
        return user;
    }

    private UserAccount createOAuthUser(String email, String name, String providerId, String profilePicUrl) {
        UserAccount user = new UserAccount();
        user.setEmail(email);
        user.setFullName(normalizeName(name, "Google User"));
        user.setPasswordHash("GOOGLE_AUTH");
        user.setRole(UserRole.CUSTOMER);
        user.setProvider(AuthProvider.GOOGLE);
        user.setProviderId(providerId);
        user.setActive(true);
        user.setProfilePicUrl(profilePicUrl);
        return user;
    }

    private String normalizeEmail(String email) {
        return email == null ? "" : email.trim().toLowerCase(Locale.ROOT);
    }

    private String normalizeName(String name, String fallback) {
        if (name == null || name.trim().isEmpty()) {
            return fallback;
        }
        return name.trim();
    }

    private String sanitizeProfilePicUrl(String profilePicUrl) {
        if (profilePicUrl == null || profilePicUrl.isBlank()) {
            return null;
        }
        String normalizedUrl = profilePicUrl.trim();
        return normalizedUrl.length() <= PROFILE_PIC_URL_MAX_LENGTH
                ? normalizedUrl
                : normalizedUrl.substring(0, PROFILE_PIC_URL_MAX_LENGTH);
    }
}
