package com.ferreteria.fc.domain.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Min;
import lombok.*;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class InventoryCountLineRequest {
    @NotNull
    private Long productoId;
    @NotNull
    @Min(0)
    private Integer cantidadContada;
    private String observaciones;
}
