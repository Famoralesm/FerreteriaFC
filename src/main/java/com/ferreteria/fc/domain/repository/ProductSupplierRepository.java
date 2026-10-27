package com.ferreteria.fc.domain.repository;

import com.ferreteria.fc.persistence.entity.ProductSupplier;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ProductSupplierRepository
        extends JpaRepository<ProductSupplier, Long> {

    List<ProductSupplier> findByProductId(Long productId);

    Optional<ProductSupplier> findByProductIdAndSelectedTrue(Long productId);

    Optional<ProductSupplier> findByProductIdAndSupplierId(
            Long productId,
            Long supplierId
    );
}
