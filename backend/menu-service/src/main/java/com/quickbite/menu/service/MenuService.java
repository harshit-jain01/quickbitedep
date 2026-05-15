package com.quickbite.menu.service;

import com.quickbite.menu.dto.MenuGroupResponse;
import java.util.List;
public interface MenuService {

    List<MenuGroupResponse> getRestaurantMenu(Long restaurantId);
}
