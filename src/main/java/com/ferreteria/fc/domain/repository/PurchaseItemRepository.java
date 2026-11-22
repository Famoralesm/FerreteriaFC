package com.ferreteria.fc.domain.repository;

import com.ferreteria.fc.persistence.entity.PurchaseItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PurchaseItemRepository extends JpaRepository<PurchaseItem, Long> {

    List<PurchaseItem> findByPurchaseIdOrderByIdAsc(Long purchaseId);
}
