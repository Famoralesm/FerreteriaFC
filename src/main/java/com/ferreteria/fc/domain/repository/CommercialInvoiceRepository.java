package com.ferreteria.fc.domain.repository;

import com.ferreteria.fc.persistence.entity.CommercialInvoice;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface CommercialInvoiceRepository extends JpaRepository<CommercialInvoice, Long> {
    @EntityGraph(attributePaths = {"store", "sourceOrder", "createdBy", "items", "items.product"})
    List<CommercialInvoice> findAllByStore_IdOrderByCreatedAtDesc(Long storeId);

    @EntityGraph(attributePaths = {"store", "sourceOrder", "createdBy", "items", "items.product"})
    List<CommercialInvoice> findAllByOrderByCreatedAtDesc();

    @EntityGraph(attributePaths = {"store", "sourceOrder", "createdBy", "items", "items.product"})
    Optional<CommercialInvoice> findDetailedById(Long id);

    boolean existsBySourceOrder_Id(Long orderId);
}
