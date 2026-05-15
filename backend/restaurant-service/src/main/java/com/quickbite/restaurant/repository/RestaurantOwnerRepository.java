package com.quickbite.restaurant.repository;

import com.quickbite.restaurant.model.RestaurantOwnerEntity;
import java.util.Optional;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface RestaurantOwnerRepository extends JpaRepository<RestaurantOwnerEntity, Long> {
    Logger logger = LoggerFactory.getLogger(RestaurantOwnerRepository.class);

    Optional<RestaurantOwnerEntity> findByUserId(UUID userId);
    Boolean existsByUserId(UUID userId);
}

