package com.quickbite.menu.dto;

import java.util.List;

public record MenuGroupResponse(
        String category,
        List<MenuItemResponse> items
) {
}
