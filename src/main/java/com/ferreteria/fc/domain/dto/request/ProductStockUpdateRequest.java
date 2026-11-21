package com.ferreteria.fc.domain.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductStockUpdateRequest {
    @NotNull
    @Min(0)
    private Integer stock;

    @NotNull
    @Min(0)
    private Integer stockMinimo;

    private String motivo;
}
