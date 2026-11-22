package com.ferreteria.fc.domain.repository;

import com.ferreteria.fc.domain.enums.PurchaseStatus;
import com.ferreteria.fc.persistence.entity.Purchase;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PurchaseRepository extends JpaRepository<Purchase, Long> {

    @EntityGraph(attributePaths = {"supplier", "sucursal"})
    List<Purchase> findAllByOrderByCreatedAtDesc();

    @EntityGraph(attributePaths = {"supplier", "sucursal"})
    List<Purchase> findBySucursalIdOrderByCreatedAtDesc(Long sucursalId);

    @EntityGraph(attributePaths = {"supplier", "sucursal"})
    List<Purchase> findBySupplierIdOrderByCreatedAtDesc(Long supplierId);

    @EntityGraph(attributePaths = {"supplier", "sucursal", "items", "items.product", "items.productUnit"})
    Optional<Purchase> findWithDetailsById(Long id);

    boolean existsByIdAndStatus(Long id, PurchaseStatus status);
}
