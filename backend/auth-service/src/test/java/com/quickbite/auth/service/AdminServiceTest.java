package com.quickbite.auth.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.quickbite.auth.client.OrderAdminClient;
import com.quickbite.auth.config.FixedAdminProperties;
import com.quickbite.auth.dto.OrderCountResponse;
import com.quickbite.auth.model.AuthProvider;
import com.quickbite.auth.model.UserAccount;
import com.quickbite.auth.model.UserRole;
import com.quickbite.auth.repository.UserRepository;
import com.quickbite.auth.service.impl.AdminServiceImpl;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AdminServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserMapper userMapper;

    @Mock
    private OrderAdminClient orderAdminClient;

    @Mock
    private FixedAdminProperties fixedAdminProperties;

    @InjectMocks
    private AdminServiceImpl adminService;

    @Test
    void getDashboard_shouldReturnUserAndOrderMetrics() {
        when(userRepository.count()).thenReturn(7L);
        when(orderAdminClient.getTotalOrders("ADMIN")).thenReturn(new OrderCountResponse(21L));

        var response = adminService.getDashboard();

        assertEquals(7L, response.totalUsers());
        assertEquals(21L, response.totalOrders());
    }

    @Test
    void getDashboard_shouldReturnZeroOrders_whenOrderServiceFails() {
        when(userRepository.count()).thenReturn(9L);
        when(orderAdminClient.getTotalOrders("ADMIN")).thenThrow(new RuntimeException("down"));

        var response = adminService.getDashboard();

        assertEquals(9L, response.totalUsers());
        assertEquals(0L, response.totalOrders());
    }

    @Test
    void getDashboard_shouldReturnZeroOrders_whenOrderResponseIsNull() {
        when(userRepository.count()).thenReturn(4L);
        when(orderAdminClient.getTotalOrders("ADMIN")).thenReturn(null);

        var response = adminService.getDashboard();

        assertEquals(0L, response.totalOrders());
    }

    @Test
    void getAllUsers_shouldMapAllUsersToAdminResponse() {
        UserAccount user = user("one@mail.com");
        when(userRepository.findAll()).thenReturn(List.of(user));

        var response = adminService.getAllUsers();

        assertEquals(1, response.size());
        assertEquals("one@mail.com", response.get(0).email());
    }

    @Test
    void deleteUser_shouldDeleteWhenNotFixedAdmin() {
        UUID userId = UUID.randomUUID();
        UserAccount user = user("customer@mail.com");
        when(fixedAdminProperties.email()).thenReturn("admin@mail.com");
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        adminService.deleteUser(userId);

        verify(userRepository).deleteById(userId);
    }

    @Test
    void deleteUser_shouldThrowWhenUserNotFound() {
        UUID userId = UUID.randomUUID();
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> adminService.deleteUser(userId));

        assertEquals("User not found", ex.getMessage());
        verify(userRepository, never()).deleteById(userId);
    }

    @Test
    void deleteUser_shouldThrowWhenTryingToDeleteFixedAdmin() {
        UUID userId = UUID.randomUUID();
        UserAccount fixedAdmin = user("admin@mail.com");
        when(fixedAdminProperties.email()).thenReturn("ADMIN@MAIL.COM");
        when(userRepository.findById(userId)).thenReturn(Optional.of(fixedAdmin));

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> adminService.deleteUser(userId));

        assertEquals("Fixed admin account cannot be deleted", ex.getMessage());
        verify(userRepository, never()).deleteById(userId);
    }

    private UserAccount user(String email) {
        UserAccount user = new UserAccount();
        user.setUserId(UUID.randomUUID());
        user.setEmail(email);
        user.setFullName("Test");
        user.setPhone("9999999999");
        user.setRole(UserRole.CUSTOMER);
        user.setProvider(AuthProvider.LOCAL);
        user.setActive(true);
        user.setCreatedAt(Instant.now());
        return user;
    }
}
