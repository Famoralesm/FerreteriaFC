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
public class ProductStockUpdateRequest {
    @NotNull
    @DecimalMin("0")
    private BigDecimal stock;

    @NotNull
    @DecimalMin("0")
    private BigDecimal stockMinimo;

    private String motivo;
}
