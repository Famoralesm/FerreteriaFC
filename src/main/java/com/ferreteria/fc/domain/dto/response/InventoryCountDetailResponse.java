package com.ferreteria.fc.domain.dto.response;

import lombok.*;
import java.math.BigDecimal;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class InventoryCountDetailResponse {
    private Long id;
    private Long productoId;
    private String sku;
    private String nombreProducto;
    private BigDecimal stockEsperado;
    private BigDecimal stockContado;
    private BigDecimal diferencia;
    private String observaciones;
    private Boolean ajusteAplicado;
}
