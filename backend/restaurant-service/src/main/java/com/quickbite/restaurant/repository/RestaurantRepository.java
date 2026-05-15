package com.quickbite.restaurant.repository;

import com.quickbite.restaurant.model.Category;
import com.quickbite.restaurant.model.Restaurant;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Repository;

@Repository
public class RestaurantRepository {

    private static final Logger logger = LoggerFactory.getLogger(RestaurantRepository.class);
    private static final Path DELETED_IDS_FILE = Paths.get(
            System.getProperty("user.home"),
            ".quickbite",
            "restaurant-service",
            "deleted-default-restaurant-ids.txt"
    );

    private final List<Category> categories = List.of(
            new Category("north-indian", "North Indian", "https://images.unsplash.com/photo-1585937421612-70a008356fbe?auto=format&fit=crop&w=400&q=80"),
            new Category("biryani", "Biryani", "https://images.unsplash.com/photo-1633945274309-2c16c9682a8d?auto=format&fit=crop&w=400&q=80"),
            new Category("south-indian", "South Indian", "https://images.unsplash.com/photo-1668236543090-82eba5ee5976?auto=format&fit=crop&w=400&q=80"),
            new Category("pizza", "Pizza", "https://images.unsplash.com/photo-1513104890138-7c749659a591?auto=format&fit=crop&w=400&q=80"),
            new Category("burger", "Burger", "https://images.unsplash.com/photo-1568901346375-23c9450c58cd?auto=format&fit=crop&w=400&q=80"),
            new Category("desserts", "Desserts", "https://images.unsplash.com/photo-1551024601-bec78aea704b?auto=format&fit=crop&w=400&q=80")
    );

    private final List<Restaurant> restaurants = new ArrayList<>(List.of(
            new Restaurant(1L, "Spice Route Kitchen", "North Indian", "Maharana Pratap Nagar", List.of("North Indian", "Thali", "Tandoor"), 4.7, 28, 450, 2.8, "https://images.unsplash.com/photo-1517248135467-4c7edcad34c4?auto=format&fit=crop&w=900&q=80", "Comfort food platters, slow-cooked curries, and smoky kebabs."),
            new Restaurant(2L, "Nawab Dum Biryani", "Biryani", "Arera Colony", List.of("Biryani", "Mughlai", "Kebabs"), 4.8, 24, 500, 3.1, "https://images.unsplash.com/photo-1563379091339-03246963d29a?auto=format&fit=crop&w=900&q=80", "Long-grain biryanis and Hyderabad-inspired gravies."),
            new Restaurant(3L, "Dosa Stories", "South Indian", "New Market", List.of("South Indian", "Breakfast", "Filter Coffee"), 4.6, 19, 300, 1.7, "https://images.unsplash.com/photo-1610192244261-3f33de3f55e4?auto=format&fit=crop&w=900&q=80", "Crisp dosas, ghee podi idlis, and all-day tiffin boxes."),
            new Restaurant(4L, "Firestone Pizza", "Pizza", "Shahpura", List.of("Pizza", "Italian", "Sides"), 4.5, 26, 550, 4.2, "https://images.unsplash.com/photo-1513104890138-7c749659a591?auto=format&fit=crop&w=900&q=80", "Wood-fired pizzas, garlic breads, and creamy pastas."),
            new Restaurant(5L, "Stacked Burgers Co.", "Burger", "10 No. Market", List.of("Burger", "Fries", "Shakes"), 4.4, 21, 400, 2.4, "https://images.unsplash.com/photo-1550547660-d9450f859349?auto=format&fit=crop&w=900&q=80", "Smash burgers, peri peri fries, and thick shakes."),
            new Restaurant(6L, "Velvet Spoon Desserts", "Desserts", "Indrapuri", List.of("Desserts", "Cakes", "Ice Cream"), 4.9, 18, 350, 2.2, "https://images.unsplash.com/photo-1578985545062-69928b1d9587?auto=format&fit=crop&w=900&q=80", "Cakes, waffles, brownie tubs, and baked cheesecakes.")
    ));
    private final Set<Long> deletedRestaurantIds = new HashSet<>();

    public RestaurantRepository() {
        loadDeletedRestaurantIds();
    }

    public List<Category> findAllCategories() {
        logger.debug("Repository category list requested");
        return categories;
    }

    public List<Restaurant> findAll(String category, String search) {
        logger.debug("Repository restaurant list requested category={} search={}", category, search);
        String normalizedCategory = category == null ? "" : category.trim().toLowerCase(Locale.ROOT);
        String normalizedSearch = search == null ? "" : search.trim().toLowerCase(Locale.ROOT);

        return restaurants.stream()
                .filter(restaurant -> !deletedRestaurantIds.contains(restaurant.id()))
                .filter(restaurant -> normalizedCategory.isBlank()
                        || restaurant.category().toLowerCase(Locale.ROOT).equals(normalizedCategory))
                .filter(restaurant -> normalizedSearch.isBlank()
                        || restaurant.name().toLowerCase(Locale.ROOT).contains(normalizedSearch)
                        || restaurant.cuisines().stream()
                        .map(value -> value.toLowerCase(Locale.ROOT))
                        .anyMatch(value -> value.contains(normalizedSearch)))
                .sorted(Comparator.comparingDouble(Restaurant::rating).reversed())
                .collect(Collectors.toCollection(ArrayList::new));
    }

    public Restaurant findById(Long restaurantId) {
        logger.debug("Repository restaurant lookup by id={}", restaurantId);
        if (deletedRestaurantIds.contains(restaurantId)) {
            throw new IllegalArgumentException("Restaurant not found");
        }
        return restaurants.stream()
                .filter(restaurant -> restaurant.id().equals(restaurantId))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Restaurant not found"));
    }

    public boolean deleteById(Long restaurantId) {
        logger.warn("Repository delete restaurant by id={}", restaurantId);
        boolean removed = restaurants.removeIf(restaurant -> restaurant.id().equals(restaurantId));
        if (removed) {
            deletedRestaurantIds.add(restaurantId);
            persistDeletedRestaurantIds();
        }
        return removed;
    }

    private void loadDeletedRestaurantIds() {
        if (!Files.exists(DELETED_IDS_FILE)) {
            return;
        }
        try {
            List<String> lines = Files.readAllLines(DELETED_IDS_FILE, StandardCharsets.UTF_8);
            for (String line : lines) {
                String trimmed = line == null ? "" : line.trim();
                if (!trimmed.isBlank()) {
                    deletedRestaurantIds.add(Long.parseLong(trimmed));
                }
            }
            logger.info("Loaded {} deleted default restaurant ids", deletedRestaurantIds.size());
        } catch (Exception ex) {
            logger.warn("Failed to load deleted default restaurant ids: {}", ex.getMessage());
        }
    }

    private void persistDeletedRestaurantIds() {
        try {
            Files.createDirectories(DELETED_IDS_FILE.getParent());
            List<String> lines = deletedRestaurantIds.stream()
                    .sorted()
                    .map(String::valueOf)
                    .toList();
            Files.write(DELETED_IDS_FILE, lines, StandardCharsets.UTF_8);
        } catch (IOException ex) {
            logger.warn("Failed to persist deleted default restaurant ids: {}", ex.getMessage());
        }
    }
}
