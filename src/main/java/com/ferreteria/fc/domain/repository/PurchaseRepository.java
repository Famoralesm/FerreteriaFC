package com.ferreteria.fc.domain.repository;

import com.ferreteria.fc.domain.enums.PurchaseStatus;
import com.ferreteria.fc.persistence.entity.Purchase;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

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

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT p FROM Purchase p WHERE p.id = :id")
    Optional<Purchase> findByIdForUpdate(@Param("id") Long id);

    boolean existsByIdAndStatus(Long id, PurchaseStatus status);
}
