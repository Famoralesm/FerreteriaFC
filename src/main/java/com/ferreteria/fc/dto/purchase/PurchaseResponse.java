package com.ferreteria.fc.dto.purchase;

import com.ferreteria.fc.domain.enums.PurchaseStatus;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Representación de una compra y sus detalles.
 */
public record PurchaseResponse(
        Long id,
        Long supplierId,
        String supplierName,
        Long sucursalId,
        String documentType,
        String documentNumber,
        LocalDate purchaseDate,
        PurchaseStatus status,
        BigDecimal total,
        String notes,
        Long createdById,
        Long receivedById,
        LocalDateTime receivedAt,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        List<PurchaseItemResponse> items
) {
}
