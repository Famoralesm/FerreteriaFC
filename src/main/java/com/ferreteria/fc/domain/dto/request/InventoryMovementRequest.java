package com.ferreteria.fc.domain.dto.request;

import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InventoryMovementRequest {

    @NotNull
    private Long productoId;

    // Solo se utilizará cuando el usuario sea ADMIN.
    private Long sucursalId;

    @NotNull
    private BigDecimal cantidad;

    private String motivo;

    private String tipoReferencia;

    private Long referenciaId;
}