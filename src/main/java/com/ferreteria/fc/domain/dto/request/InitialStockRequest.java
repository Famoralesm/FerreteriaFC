package com.ferreteria.fc.domain.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InitialStockRequest {

    @NotNull
    private Long productoId;

    // Obligatoria para ADMIN; los demás roles usan su sucursal asignada.
    private Long sucursalId;

    @NotNull
    @DecimalMin(value = "0.0", inclusive = true)
    private BigDecimal stock;

    @NotNull
    @DecimalMin(value = "0.0", inclusive = true)
    private BigDecimal stockMinimo;

    private String motivo;
}
