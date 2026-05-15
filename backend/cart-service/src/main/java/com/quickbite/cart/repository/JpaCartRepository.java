package com.quickbite.cart.repository;

import com.quickbite.cart.model.CartEntity;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface JpaCartRepository extends JpaRepository<CartEntity, Long> {

    Optional<CartEntity> findByUserEmail(String userEmail);

    void deleteByUserEmail(String userEmail);
}

