package com.ferreteria.fc.domain.repository;

import com.ferreteria.fc.persistence.entity.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.util.Optional;

public interface ProductRepository
        extends JpaRepository<Product, Long> {

    Optional<Product> findBySku(String sku);

    List<Product> findBySkuContainingIgnoreCase(String sku);

    boolean existsBySku(String sku);

    boolean existsByBaseUnitId(Long baseUnitId);

    List<Product> findByActiveTrue();

    List<Product> findByCategory(String category);

    List<Product> findByCategoryAndActiveTrue(String category);

    List<Product> findBySkuContainingIgnoreCaseOrNameContainingIgnoreCase(
            String sku,
            String name
    );

    long countByImagePath(String imagePath);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT p FROM Product p WHERE p.id = :id")
    Optional<Product> findByIdForUpdate(@Param("id") Long id);
}