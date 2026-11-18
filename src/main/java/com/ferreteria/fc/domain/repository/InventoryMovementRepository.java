
package com.ferreteria.fc.domain.repository;

import com.ferreteria.fc.domain.enums.MovementType;
import com.ferreteria.fc.persistence.entity.InventoryMovement;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface InventoryMovementRepository
        extends JpaRepository<InventoryMovement, Long> {

    Page<InventoryMovement> findByProductIdOrderByCreatedAtDesc(
            Long productId,
            Pageable pageable
    );

    List<InventoryMovement> findByProductIdOrderByCreatedAtDesc(
            Long productId
    );

    boolean existsByProductIdAndMovementTypeAndReferenceTypeAndReferenceId(
            Long productId,
            MovementType movementType,
            String referenceType,
            Long referenceId
    );

    List<InventoryMovement> findByReferenceTypeAndReferenceId(
            String referenceType,
            Long referenceId
    );
}
