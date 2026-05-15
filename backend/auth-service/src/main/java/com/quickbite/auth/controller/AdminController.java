package com.quickbite.auth.controller;

import com.quickbite.auth.dto.AdminDashboardResponse;
import com.quickbite.auth.dto.AdminUserResponse;
import com.quickbite.auth.service.AdminService;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin")
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

    private static final Logger logger = LoggerFactory.getLogger(AdminController.class);

    private final AdminService adminService;

    public AdminController(AdminService adminService) {
        this.adminService = adminService;
    }

    @GetMapping("/dashboard")
    public AdminDashboardResponse getDashboard() {
        logger.info("Admin dashboard request received");
        return adminService.getDashboard();
    }

    @GetMapping("/users")
    public List<AdminUserResponse> getUsers() {
        logger.info("Admin users list request received");
        return adminService.getAllUsers();
    }

    @DeleteMapping("/users/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteUser(@PathVariable("id") UUID userId) {
        logger.warn("Admin delete user request received for userId={}", userId);
        adminService.deleteUser(userId);
    }
}

