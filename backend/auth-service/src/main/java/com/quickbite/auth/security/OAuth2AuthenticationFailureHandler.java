package com.quickbite.auth.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationFailureHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

@Component
public class OAuth2AuthenticationFailureHandler extends SimpleUrlAuthenticationFailureHandler {

    private final String redirectUri;
    private final ObjectMapper objectMapper;

    public OAuth2AuthenticationFailureHandler(
            @Value("${app.oauth2.authorized-redirect-uri}") String redirectUri,
            ObjectMapper objectMapper
    ) {
        this.redirectUri = redirectUri;
        this.objectMapper = objectMapper;
    }

    @Override
    public void onAuthenticationFailure(
            HttpServletRequest request,
            HttpServletResponse response,
            AuthenticationException exception
    ) throws IOException, ServletException {
        if (redirectUri != null && !redirectUri.isBlank()) {
            String targetUrl = UriComponentsBuilder
                    .fromUriString(redirectUri)
                    .queryParam("error", "oauth2_login_failed")
                    .build()
                    .toUriString();

            getRedirectStrategy().sendRedirect(request, response, targetUrl);
            return;
        }

        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json");
        response.getWriter().write(objectMapper.writeValueAsString(Map.of(
                "message", "OAuth2 login failed",
                "details", exception.getMessage() != null ? exception.getMessage() : "Authentication failed"
        )));
    }
}

