package com.quickbite.auth.repository;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.quickbite.auth.model.AuthProvider;
import com.quickbite.auth.model.UserAccount;
import com.quickbite.auth.model.UserRole;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

@DataJpaTest
class UserRepositoryTest {

    @Autowired
    private UserRepository userRepository;

    @Test
    void findByEmailIgnoreCase_shouldFindUser_regardlessOfCase() {
        userRepository.save(buildUser("case.user@mail.com"));

        boolean found = userRepository.findByEmailIgnoreCase("CASE.USER@MAIL.COM").isPresent();

        assertTrue(found);
    }

    @Test
    void existsByEmailIgnoreCase_shouldReturnCorrectFlags() {
        userRepository.save(buildUser("exists@mail.com"));

        assertTrue(userRepository.existsByEmailIgnoreCase("EXISTS@mail.com"));
        assertFalse(userRepository.existsByEmailIgnoreCase("absent@mail.com"));
    }

    private UserAccount buildUser(String email) {
        UserAccount user = new UserAccount();
        user.setUserId(UUID.randomUUID());
        user.setFullName("Repo User");
        user.setEmail(email);
        user.setPasswordHash("ENC");
        user.setPhone("9999999999");
        user.setRole(UserRole.CUSTOMER);
        user.setProvider(AuthProvider.LOCAL);
        user.setActive(true);
        user.setCreatedAt(Instant.now());
        return user;
    }
}
