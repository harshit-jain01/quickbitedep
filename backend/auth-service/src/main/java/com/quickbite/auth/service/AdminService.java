package com.quickbite.auth.service;

import com.quickbite.auth.dto.AdminDashboardResponse;
import com.quickbite.auth.dto.AdminUserResponse;
import java.util.List;
import java.util.UUID;
public interface AdminService {

    AdminDashboardResponse getDashboard();

    List<AdminUserResponse> getAllUsers();

    void deleteUser(UUID userId);
}

