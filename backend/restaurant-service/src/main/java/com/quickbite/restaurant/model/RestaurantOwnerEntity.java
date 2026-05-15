package com.quickbite.restaurant.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "restaurant_owners")
@Data
@NoArgsConstructor
public class RestaurantOwnerEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private UUID userId;

    @Column(nullable = false)
    private String restaurantName;

    @Column
    private String cuisineType;

    @Column
    private String address;

    @Column
    private String city;

    @Column
    private String state;

    @Column
    private String postalCode;

    @Column
    private String phone;

    @Column
    private String email;

    @Column
    private String website;

    @Column(columnDefinition = "LONGTEXT")
    private String imageUrl;

    @Column(name = "min_order_amount")
    private Double minimumOrderAmount = 0.0;

    @Column(name = "estimated_delivery_time")
    private Integer estimatedDeliveryTime = 30;

    @Column(name = "is_active")
    private Boolean isActive = true;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at")
    private LocalDateTime updatedAt = LocalDateTime.now();

    public RestaurantOwnerEntity(UUID userId, String restaurantName, String cuisineType, String address) {
        this.userId = userId;
        this.restaurantName = restaurantName;
        this.cuisineType = cuisineType;
        this.address = address;
    }
}
