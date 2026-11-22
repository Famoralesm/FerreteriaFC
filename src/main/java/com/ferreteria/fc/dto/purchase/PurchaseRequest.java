package com.ferreteria.fc.dto.purchase;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.util.List;

/**
 * Datos necesarios para registrar una compra pendiente de recepción.
 * El backend calcula los subtotales y el total, y obtiene las conversiones
 * desde el catálogo; esos valores no se reciben desde el cliente.
 */
public record PurchaseRequest(

        @NotNull(message = "El proveedor es obligatorio")
        @Positive(message = "El ID del proveedor debe ser válido")
        Long supplierId,

        @NotNull(message = "La sucursal es obligatoria")
        @Positive(message = "El ID de la sucursal debe ser válido")
        Long sucursalId,

        @Size(max = 30, message = "El tipo de documento no puede superar 30 caracteres")
        String documentType,

        @Size(max = 60, message = "El número de documento no puede superar 60 caracteres")
        String documentNumber,

        @NotNull(message = "La fecha de compra es obligatoria")
        LocalDate purchaseDate,

        @Size(max = 1000, message = "Las observaciones no pueden superar 1000 caracteres")
        String notes,

        @NotEmpty(message = "La compra debe incluir al menos un producto")
        List<@Valid PurchaseItemRequest> items
) {
}
