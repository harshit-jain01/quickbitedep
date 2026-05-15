package com.quickbite.menu.repository;

import com.quickbite.menu.model.MenuItem;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Repository;

@Repository
public class MenuRepository {

    private static final Logger logger = LoggerFactory.getLogger(MenuRepository.class);

    private final Map<Long, List<MenuItem>> menuByRestaurant = Map.of(
            1L, List.of(
                    new MenuItem(101L, 1L, "Royal North Indian Thali", "Paneer, dal makhani, naan, pulao and dessert.", "Recommended", 289, true, true, "https://images.unsplash.com/photo-1601050690597-df0568f70950?auto=format&fit=crop&w=700&q=80"),
                    new MenuItem(102L, 1L, "Butter Chicken Bowl", "Creamy tomato gravy with smoked tandoori chicken.", "Recommended", 325, false, true, "https://images.unsplash.com/photo-1603894584373-5ac82b2ae398?auto=format&fit=crop&w=700&q=80"),
                    new MenuItem(103L, 1L, "Tandoori Paneer Tikka", "Charred cottage cheese skewers with mint chutney.", "Starters", 249, true, false, "https://images.unsplash.com/photo-1567188040759-fb8a883dc6d8?auto=format&fit=crop&w=700&q=80")
            ),
            2L, List.of(
                    new MenuItem(201L, 2L, "Hyderabadi Chicken Biryani", "Layered dum biryani with saffron and raita.", "Biryani", 349, false, true, "https://images.unsplash.com/photo-1631515243349-e0cb75fb8d3a?auto=format&fit=crop&w=700&q=80"),
                    new MenuItem(202L, 2L, "Paneer Tikka Biryani", "Rich paneer biryani with masala onions.", "Biryani", 309, true, true, "https://images.unsplash.com/photo-1642821373181-696a54913e93?auto=format&fit=crop&w=700&q=80"),
                    new MenuItem(203L, 2L, "Mutton Seekh Kebab", "Juicy kebabs with rumali roti.", "Sides", 369, false, false, "https://images.unsplash.com/photo-1529006557810-274b9b2fc783?auto=format&fit=crop&w=700&q=80")
            ),
            3L, List.of(
                    new MenuItem(301L, 3L, "Masala Dosa", "Classic dosa with potato filling and chutneys.", "Breakfast", 159, true, true, "https://images.unsplash.com/photo-1668236543090-82eba5ee5976?auto=format&fit=crop&w=700&q=80"),
                    new MenuItem(302L, 3L, "Podi Idli", "Mini idlis tossed in spicy gunpowder.", "Breakfast", 129, true, false, "https://images.unsplash.com/photo-1626074353765-517a681e40be?auto=format&fit=crop&w=700&q=80"),
                    new MenuItem(303L, 3L, "Filter Coffee", "Strong South Indian filter coffee.", "Beverages", 79, true, false, "https://images.unsplash.com/photo-1495474472287-4d71bcdd2085?auto=format&fit=crop&w=700&q=80")
            ),
            4L, List.of(
                    new MenuItem(401L, 4L, "Farmhouse Pizza", "Bell peppers, onions, olives and mozzarella.", "Pizzas", 379, true, true, "https://images.unsplash.com/photo-1513104890138-7c749659a591?auto=format&fit=crop&w=700&q=80"),
                    new MenuItem(402L, 4L, "Pepperoni Blast", "Pepperoni, cheese and roasted garlic.", "Pizzas", 429, false, true, "https://images.unsplash.com/photo-1628840042765-356cda07504e?auto=format&fit=crop&w=700&q=80"),
                    new MenuItem(403L, 4L, "Creamy Alfredo Pasta", "Penne pasta in parmesan cream sauce.", "Pasta", 299, true, false, "https://images.unsplash.com/photo-1645112411341-6c4fd023714a?auto=format&fit=crop&w=700&q=80")
            ),
            5L, List.of(
                    new MenuItem(501L, 5L, "Classic Smash Burger", "Double patty burger with cheddar and onions.", "Burgers", 249, false, true, "https://images.unsplash.com/photo-1568901346375-23c9450c58cd?auto=format&fit=crop&w=700&q=80"),
                    new MenuItem(502L, 5L, "Crispy Veg Burger", "Crispy veg patty with chipotle mayo.", "Burgers", 199, true, false, "https://images.unsplash.com/photo-1525059696034-4967a8e1dca2?auto=format&fit=crop&w=700&q=80"),
                    new MenuItem(503L, 5L, "Peri Peri Fries", "Seasoned fries with spicy dusting.", "Sides", 139, true, false, "https://images.unsplash.com/photo-1576107232684-1279f390859f?auto=format&fit=crop&w=700&q=80")
            ),
            6L, List.of(
                    new MenuItem(601L, 6L, "Chocolate Truffle Pastry", "Dense chocolate pastry with ganache.", "Desserts", 149, true, true, "https://images.unsplash.com/photo-1578985545062-69928b1d9587?auto=format&fit=crop&w=700&q=80"),
                    new MenuItem(602L, 6L, "Belgian Waffle", "Warm waffle with maple and vanilla ice cream.", "Desserts", 219, true, false, "https://images.unsplash.com/photo-1562376552-0d160a2f238d?auto=format&fit=crop&w=700&q=80"),
                    new MenuItem(603L, 6L, "Nutella Brownie Tub", "Fudgy brownie topped with hazelnut spread.", "Desserts", 179, true, false, "https://images.unsplash.com/photo-1606313564200-e75d5e30476c?auto=format&fit=crop&w=700&q=80")
            )
    );

    public List<MenuItem> findByRestaurantId(Long restaurantId) {
        logger.debug("Repository static menu lookup for restaurantId={}", restaurantId);
        return menuByRestaurant.getOrDefault(restaurantId, List.of());
    }
}
