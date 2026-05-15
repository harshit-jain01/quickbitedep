package com.quickbite.auth.config;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.quickbite.auth.model.UserRole;
import com.quickbite.auth.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.DefaultApplicationArguments;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class FixedAdminInitializerTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private FixedAdminProperties fixedAdminProperties;

    @InjectMocks
    private FixedAdminInitializer initializer;

    @Test
    void run_shouldCreateAdmin_whenNotPresent() throws Exception {
        when(fixedAdminProperties.email()).thenReturn("  ADMIN@MAIL.COM ");
        when(fixedAdminProperties.password()).thenReturn("admin123");
        when(fixedAdminProperties.fullName()).thenReturn("  Super Admin ");
        when(userRepository.existsByEmailIgnoreCase("admin@mail.com")).thenReturn(false);
        when(passwordEncoder.encode("admin123")).thenReturn("ENC");

        initializer.run(new DefaultApplicationArguments(new String[0]));

        ArgumentCaptor<com.quickbite.auth.model.UserAccount> userCaptor =
                ArgumentCaptor.forClass(com.quickbite.auth.model.UserAccount.class);
        verify(userRepository).save(userCaptor.capture());
        var saved = userCaptor.getValue();
        org.junit.jupiter.api.Assertions.assertEquals("admin@mail.com", saved.getEmail());
        org.junit.jupiter.api.Assertions.assertEquals("Super Admin", saved.getFullName());
        org.junit.jupiter.api.Assertions.assertEquals(UserRole.ADMIN, saved.getRole());
    }

    @Test
    void run_shouldSkipCreation_whenAdminAlreadyExists() throws Exception {
        when(fixedAdminProperties.email()).thenReturn("admin@mail.com");
        when(userRepository.existsByEmailIgnoreCase("admin@mail.com")).thenReturn(true);

        initializer.run(new DefaultApplicationArguments(new String[0]));

        verify(userRepository, never()).save(any());
        verify(passwordEncoder, never()).encode(any());
    }
}
