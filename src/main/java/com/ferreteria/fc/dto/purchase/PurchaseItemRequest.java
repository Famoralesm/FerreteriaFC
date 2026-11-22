package com.ferreteria.fc.dto.purchase;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

/**
 * Datos enviados para agregar un producto a una compra.
 * productUnitId puede ser null cuando se compra en la unidad base.
 */
public record PurchaseItemRequest(

        @NotNull(message = "El producto es obligatorio")
        @Positive(message = "El ID del producto debe ser válido")
        Long productId,

        Long productUnitId,

        @NotNull(message = "La cantidad es obligatoria")
        @DecimalMin(value = "0.0001", message = "La cantidad debe ser mayor que cero")
        BigDecimal quantity,

        @NotNull(message = "El precio unitario de compra es obligatorio")
        @DecimalMin(value = "0.00", message = "El precio no puede ser negativo")
        BigDecimal unitPurchasePrice
) {
}
