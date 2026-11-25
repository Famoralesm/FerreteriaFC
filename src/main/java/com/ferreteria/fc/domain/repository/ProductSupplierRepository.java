package com.ferreteria.fc.domain.repository;

import com.ferreteria.fc.persistence.entity.ProductSupplier;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.EntityGraph;

import java.util.List;
import java.util.Optional;

public interface ProductSupplierRepository
        extends JpaRepository<ProductSupplier, Long> {

    List<ProductSupplier> findByProductId(Long productId);

    @EntityGraph(attributePaths = {"product", "supplier"})
    List<ProductSupplier> findByProductIdOrderByPurchasePriceAsc(Long productId);

    @EntityGraph(attributePaths = {"product", "supplier"})
    List<ProductSupplier> findBySupplierIdOrderByPurchasePriceAsc(Long supplierId);

    Optional<ProductSupplier> findByProductIdAndSelectedTrue(Long productId);

    Optional<ProductSupplier> findByProductIdAndSupplierIdAndBrandIdAndProductUnitId(
            Long productId, Long supplierId, Long brandId, Long productUnitId
    );
}
