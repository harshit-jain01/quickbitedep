package com.quickbite.auth.security;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.quickbite.auth.dto.UserProfileResponse;
import com.quickbite.auth.model.AuthProvider;
import com.quickbite.auth.model.UserAccount;
import com.quickbite.auth.model.UserRole;
import com.quickbite.auth.repository.UserRepository;
import com.quickbite.auth.service.UserMapper;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.RedirectStrategy;

@ExtendWith(MockitoExtension.class)
class OAuth2AuthenticationSuccessHandlerTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private JwtService jwtService;

    @Mock
    private UserMapper userMapper;

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    @Mock
    private Authentication authentication;

    @Mock
    private RedirectStrategy redirectStrategy;

    private ObjectMapper objectMapper;
    private OAuth2AuthenticationSuccessHandler handlerWithRedirect;
    private OAuth2AuthenticationSuccessHandler handlerWithoutRedirect;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        handlerWithRedirect = new OAuth2AuthenticationSuccessHandler(
                userRepository, jwtService, userMapper, objectMapper, "http://localhost:3000/oauth2/redirect"
        );
        handlerWithRedirect.setRedirectStrategy(redirectStrategy);

        handlerWithoutRedirect = new OAuth2AuthenticationSuccessHandler(
                userRepository, jwtService, userMapper, objectMapper, null
        );
    }

    @Test
    void onAuthenticationSuccess_shouldThrowWhenEmailIsMissing() {
        OAuth2User oAuth2User = new DefaultOAuth2User(null, Map.of("sub", "12345"), "sub");
        when(authentication.getPrincipal()).thenReturn(oAuth2User);

        assertThrows(ServletException.class, () -> handlerWithRedirect.onAuthenticationSuccess(request, response, authentication));
    }

    @Test
    void onAuthenticationSuccess_shouldCreateNewUserAndRedirect() throws IOException, ServletException {
        OAuth2User oAuth2User = new DefaultOAuth2User(null, Map.of(
                "sub", "12345",
                "email", "test@google.com",
                "name", "Test User",
                "picture", "http://pic.com/1.png"
        ), "sub");
        when(authentication.getPrincipal()).thenReturn(oAuth2User);
        when(userRepository.findByEmailIgnoreCase("test@google.com")).thenReturn(Optional.empty());
        when(jwtService.generateToken(any(UserAccount.class))).thenReturn("mock-token");

        handlerWithRedirect.onAuthenticationSuccess(request, response, authentication);

        ArgumentCaptor<UserAccount> userCaptor = ArgumentCaptor.forClass(UserAccount.class);
        verify(userRepository).save(userCaptor.capture());
        UserAccount capturedUser = userCaptor.getValue();
        assertTrue(capturedUser.getEmail().equals("test@google.com"));
        assertTrue(capturedUser.getFullName().equals("Test User"));
        assertTrue(capturedUser.getProfilePicUrl().equals("http://pic.com/1.png"));
        assertTrue(capturedUser.getProvider() == AuthProvider.GOOGLE);
        assertTrue(capturedUser.getRole() == UserRole.CUSTOMER);

        verify(redirectStrategy).sendRedirect(request, response, "http://localhost:3000/oauth2/redirect?token=mock-token");
    }

    @Test
    void onAuthenticationSuccess_shouldUpdateExistingUserAndWriteJson() throws IOException, ServletException {
        OAuth2User oAuth2User = new DefaultOAuth2User(null, Map.of(
                "sub", "12345",
                "email", "existing@google.com",
                "name", "Existing User"
        ), "sub");
        when(authentication.getPrincipal()).thenReturn(oAuth2User);

        UserAccount existingUser = new UserAccount();
        existingUser.setEmail("existing@google.com");
        existingUser.setFullName("Old Name");
        existingUser.setProvider(AuthProvider.LOCAL);

        when(userRepository.findByEmailIgnoreCase("existing@google.com")).thenReturn(Optional.of(existingUser));
        when(jwtService.generateToken(any(UserAccount.class))).thenReturn("mock-token-2");
        when(jwtService.getExpirationSeconds()).thenReturn(3600L);
        when(userMapper.toProfile(any(UserAccount.class))).thenReturn(new UserProfileResponse(
                UUID.randomUUID(), "Existing User", "existing@google.com", null, UserRole.CUSTOMER, AuthProvider.GOOGLE, true, Instant.now(), null
        ));

        StringWriter stringWriter = new StringWriter();
        PrintWriter printWriter = new PrintWriter(stringWriter);
        when(response.getWriter()).thenReturn(printWriter);

        handlerWithoutRedirect.onAuthenticationSuccess(request, response, authentication);

        ArgumentCaptor<UserAccount> userCaptor = ArgumentCaptor.forClass(UserAccount.class);
        verify(userRepository).save(userCaptor.capture());
        UserAccount updatedUser = userCaptor.getValue();
        assertTrue(updatedUser.getFullName().equals("Existing User"));
        assertTrue(updatedUser.getProvider() == AuthProvider.GOOGLE);
        assertTrue(updatedUser.getProviderId().equals("12345"));

        verify(response).setContentType("application/json");
        String output = stringWriter.toString();
        assertTrue(output.contains("mock-token-2"));
        assertTrue(output.contains("Bearer"));
    }
}
