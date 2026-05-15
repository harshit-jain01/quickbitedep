package com.quickbite.auth.config;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@ConfigurationProperties(prefix = "app.fixed-admin")
@Validated
public record FixedAdminProperties(
        @NotBlank @Email String email,
        @NotBlank String password,
        @NotBlank String fullName
) {
}

