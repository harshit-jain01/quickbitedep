package com.quickbite.auth.service.impl;

import com.quickbite.auth.client.OrderAdminClient;
import com.quickbite.auth.config.FixedAdminProperties;
import com.quickbite.auth.dto.AdminDashboardResponse;
import com.quickbite.auth.dto.AdminUserResponse;
import com.quickbite.auth.dto.OrderCountResponse;
import com.quickbite.auth.model.UserAccount;
import com.quickbite.auth.repository.UserRepository;
import com.quickbite.auth.service.AdminService;
import com.quickbite.auth.service.UserMapper;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class AdminServiceImpl implements AdminService {

    private static final Logger logger = LoggerFactory.getLogger(AdminServiceImpl.class);

    private final UserRepository userRepository;
    private final UserMapper userMapper;
    private final OrderAdminClient orderAdminClient;
    private final FixedAdminProperties fixedAdminProperties;

    public AdminServiceImpl(
            UserRepository userRepository,
            UserMapper userMapper,
            OrderAdminClient orderAdminClient,
            FixedAdminProperties fixedAdminProperties
    ) {
        this.userRepository = userRepository;
        this.userMapper = userMapper;
        this.orderAdminClient = orderAdminClient;
        this.fixedAdminProperties = fixedAdminProperties;
    }

    @Override
    @Transactional(readOnly = true)
    public AdminDashboardResponse getDashboard() {
        logger.info("Building admin dashboard metrics");
        long totalUsers = userRepository.count();
        long totalOrders = fetchTotalOrders();
        return new AdminDashboardResponse(totalUsers, totalOrders);
    }

    @Override
    @Transactional(readOnly = true)
    public List<AdminUserResponse> getAllUsers() {
        logger.debug("Fetching all users for admin view");
        return userRepository.findAll().stream()
                .map(this::toAdminUser)
                .toList();
    }

    @Override
    public void deleteUser(UUID userId) {
        logger.warn("Deleting user requested for userId={}", userId);
        UserAccount user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        String fixedAdminEmail = fixedAdminProperties.email().trim().toLowerCase(Locale.ROOT);
        if (user.getEmail().equalsIgnoreCase(fixedAdminEmail)) {
            throw new IllegalArgumentException("Fixed admin account cannot be deleted");
        }

        userRepository.deleteById(userId);
        logger.info("User deleted successfully for userId={}", userId);
    }

    private AdminUserResponse toAdminUser(UserAccount user) {
        return new AdminUserResponse(
                user.getUserId(),
                user.getFullName(),
                user.getEmail(),
                user.getPhone(),
                user.getRole(),
                user.isActive(),
                user.getCreatedAt()
        );
    }

    private long fetchTotalOrders() {
        try {
            logger.debug("Fetching total orders from order-service");
            OrderCountResponse response = orderAdminClient.getTotalOrders("ADMIN");
            return response == null ? 0L : response.totalOrders();
        } catch (Exception ex) {
            logger.error("Failed to fetch total orders from order-service", ex);
            return 0L;
        }
    }
}

