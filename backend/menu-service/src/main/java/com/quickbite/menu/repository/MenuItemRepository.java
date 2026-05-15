package com.quickbite.menu.repository;

import com.quickbite.menu.model.MenuItemEntity;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface MenuItemRepository extends JpaRepository<MenuItemEntity, Long> {
    Logger logger = LoggerFactory.getLogger(MenuItemRepository.class);

    List<MenuItemEntity> findByRestaurantIdAndCategoryId(Long restaurantId, Long categoryId);
    List<MenuItemEntity> findByRestaurantId(Long restaurantId);
    List<MenuItemEntity> findByRestaurantIdAndIsAvailableTrue(Long restaurantId);

    @Query("SELECT m FROM MenuItemEntity m WHERE m.restaurantId = :restaurantId AND (m.name LIKE %:search% OR m.description LIKE %:search%)")
    List<MenuItemEntity> searchByName(Long restaurantId, String search);
}

