package com.ferreteria.fc.domain.repository;

import com.ferreteria.fc.persistence.entity.AuditLog;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.repository.Repository;

public interface AuditLogRepository extends Repository<AuditLog, Long>, JpaSpecificationExecutor<AuditLog> {

    AuditLog save(AuditLog event);
}
