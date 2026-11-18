package com.ferreteria.fc.domain.repository;

import com.ferreteria.fc.domain.enums.InventoryCountStatus;
import com.ferreteria.fc.persistence.entity.InventoryCount;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface InventoryCountRepository extends JpaRepository<InventoryCount, Long> {
    List<InventoryCount> findByStatusOrderByCreatedAtDesc(InventoryCountStatus status);
    List<InventoryCount> findAllByOrderByCreatedAtDesc();
    boolean existsBySucursalIdAndStatus(Long sucursalId, InventoryCountStatus status);
    List<InventoryCount> findBySucursalIdOrderByCreatedAtDesc(Long sucursalId);
}
