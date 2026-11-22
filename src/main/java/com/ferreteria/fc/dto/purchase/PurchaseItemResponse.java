package com.ferreteria.fc.dto.purchase;

import java.math.BigDecimal;

/**
 * Detalle de un producto registrado en una compra.
 * Los snapshots representan los datos históricos guardados al registrarla.
 */
public record PurchaseItemResponse(
        Long id,
        Long productId,
        Long productUnitId,
        String productSku,
        String productName,
        String unitCode,
        String unitName,
        BigDecimal conversionFactor,
        BigDecimal quantity,
        BigDecimal quantityBaseUnit,
        BigDecimal unitPurchasePrice,
        BigDecimal subtotal
) {
}
