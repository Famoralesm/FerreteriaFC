package com.ferreteria.fc.domain.repository;

import com.ferreteria.fc.persistence.entity.AuditLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;

public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {

    @Query("""
            SELECT log
            FROM AuditLog log
            WHERE (:module IS NULL OR log.module = :module)
              AND (:entityType IS NULL OR log.entityType = :entityType)
              AND (:entityId IS NULL OR log.entityId = :entityId)
              AND (:userId IS NULL OR log.user.id = :userId)
              AND (:storeId IS NULL OR log.store.id = :storeId)
              AND (:action IS NULL OR log.action = :action)
              AND (:fromDate IS NULL OR log.createdAt >= :fromDate)
              AND (:toDate IS NULL OR log.createdAt <= :toDate)
            ORDER BY log.createdAt DESC
            """)
    Page<AuditLog> search(
            @Param("module") String module,
            @Param("entityType") String entityType,
            @Param("entityId") String entityId,
            @Param("userId") Long userId,
            @Param("storeId") Long storeId,
            @Param("action") String action,
            @Param("fromDate") LocalDateTime fromDate,
            @Param("toDate") LocalDateTime toDate,
            Pageable pageable
    );
}
