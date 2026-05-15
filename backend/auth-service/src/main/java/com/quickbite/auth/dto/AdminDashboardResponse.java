package com.quickbite.auth.dto;

public record AdminDashboardResponse(
        long totalUsers,
        long totalOrders
) {
}

