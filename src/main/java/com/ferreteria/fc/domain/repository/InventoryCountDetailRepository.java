
package com.ferreteria.fc.domain.repository;

import com.ferreteria.fc.persistence.entity.InventoryCountDetail;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface InventoryCountDetailRepository
        extends JpaRepository<InventoryCountDetail, Long> {

    List<InventoryCountDetail> findByInventoryCountId(Long inventoryCountId);

    Optional<InventoryCountDetail> findByInventoryCountIdAndProductId(
            Long inventoryCountId,
            Long productId
    );

    boolean existsByInventoryCountIdAndProductId(
            Long inventoryCountId,
            Long productId
    );
}
