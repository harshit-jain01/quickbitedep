package com.quickbite.auth.config;

import com.quickbite.auth.model.AuthProvider;
import com.quickbite.auth.model.UserAccount;
import com.quickbite.auth.model.UserRole;
import com.quickbite.auth.repository.UserRepository;
import java.util.Locale;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class FixedAdminInitializer implements ApplicationRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final FixedAdminProperties fixedAdminProperties;

    public FixedAdminInitializer(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            FixedAdminProperties fixedAdminProperties
    ) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.fixedAdminProperties = fixedAdminProperties;
    }

    @Override
    public void run(ApplicationArguments args) {
        String normalizedEmail = fixedAdminProperties.email().trim().toLowerCase(Locale.ROOT);
        if (userRepository.existsByEmailIgnoreCase(normalizedEmail)) {
            return;
        }

        UserAccount admin = new UserAccount();
        admin.setFullName(fixedAdminProperties.fullName().trim());
        admin.setEmail(normalizedEmail);
        admin.setPasswordHash(passwordEncoder.encode(fixedAdminProperties.password()));
        admin.setRole(UserRole.ADMIN);
        admin.setProvider(AuthProvider.LOCAL);
        admin.setActive(true);
        userRepository.save(admin);
    }
}

