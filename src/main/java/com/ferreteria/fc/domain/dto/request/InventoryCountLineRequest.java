package com.ferreteria.fc.domain.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.DecimalMin;
import java.math.BigDecimal;
import lombok.*;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class InventoryCountLineRequest {
    @NotNull
    private Long productoId;
    @NotNull
    @DecimalMin("0")
    private BigDecimal cantidadContada;
    private String observaciones;
}
