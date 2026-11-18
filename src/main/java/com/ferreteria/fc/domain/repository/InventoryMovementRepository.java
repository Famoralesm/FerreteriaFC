
package com.ferreteria.fc.domain.repository;

import com.ferreteria.fc.domain.enums.MovementType;
import com.ferreteria.fc.persistence.entity.InventoryMovement;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface InventoryMovementRepository
        extends JpaRepository<InventoryMovement, Long> {

    List<InventoryMovement> findByProductoIdOrderByFechaCreacionDesc(
            Long productoId
    );

    boolean existsByProductoIdAndTipoMovimientoAndTipoReferenciaAndReferenciaId(
            Long productoId,
            MovementType tipoMovimiento,
            String tipoReferencia,
            Long referenciaId
    );

    List<InventoryMovement> findByTipoReferenciaAndReferenciaId(
            String tipoReferencia,
            Long referenciaId
    );
}
