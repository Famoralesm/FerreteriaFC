package com.ferreteria.fc.domain.repository;

import com.ferreteria.fc.persistence.entity.Product;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ProductRepository
        extends JpaRepository<Product, Long> {

    Optional<Product> findBySku(String sku);

    boolean existsBySku(String sku);

    List<Product> findByActiveTrue();

    List<Product> findByCategory(String category);

    List<Product> findByCategoryAndActiveTrue(String category);

    List<Product> findBySkuContainingIgnoreCaseOrNameContainingIgnoreCase(
            String sku,
            String name
    );
}