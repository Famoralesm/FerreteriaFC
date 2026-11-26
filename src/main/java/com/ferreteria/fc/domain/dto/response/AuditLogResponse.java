package com.ferreteria.fc.domain.dto.response;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

public record AuditLogResponse(
        Long id,
        UUID eventUuid,
        Long userId,
        String userName,
        Long storeId,
        String storeName,
        String module,
        String entityType,
        String entityId,
        String action,
        Map<String, Object> oldValues,
        Map<String, Object> newValues,
        String description,
        String origin,
        LocalDateTime createdAt
) {
}
