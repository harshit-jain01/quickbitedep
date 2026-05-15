package com.quickbite.auth.security;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.web.RedirectStrategy;

@ExtendWith(MockitoExtension.class)
class OAuth2AuthenticationFailureHandlerTest {

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    @Mock
    private RedirectStrategy redirectStrategy;

    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
    }

    @Test
    void onAuthenticationFailure_shouldRedirectWhenUriIsConfigured() throws IOException, ServletException {
        OAuth2AuthenticationFailureHandler handler = new OAuth2AuthenticationFailureHandler(
                "http://localhost:3000/login", objectMapper
        );
        handler.setRedirectStrategy(redirectStrategy);

        AuthenticationException exception = new OAuth2AuthenticationException("error");

        handler.onAuthenticationFailure(request, response, exception);

        verify(redirectStrategy).sendRedirect(request, response, "http://localhost:3000/login?error=oauth2_login_failed");
    }

    @Test
    void onAuthenticationFailure_shouldWriteJsonWhenUriIsNotConfigured() throws IOException, ServletException {
        OAuth2AuthenticationFailureHandler handler = new OAuth2AuthenticationFailureHandler(
                null, objectMapper
        );

        AuthenticationException exception = new OAuth2AuthenticationException("Login failed");

        StringWriter stringWriter = new StringWriter();
        PrintWriter printWriter = new PrintWriter(stringWriter);
        when(response.getWriter()).thenReturn(printWriter);

        handler.onAuthenticationFailure(request, response, exception);

        verify(response).setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        verify(response).setContentType("application/json");

        String output = stringWriter.toString();
        assertTrue(output.contains("OAuth2 login failed"));
        assertTrue(output.contains("Authentication failed"));
    }
}
