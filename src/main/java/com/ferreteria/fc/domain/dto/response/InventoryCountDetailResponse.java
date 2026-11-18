package com.ferreteria.fc.domain.dto.response;

import lombok.*;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class InventoryCountDetailResponse {
    private Long id;
    private Long productoId;
    private String sku;
    private String nombreProducto;
    private Integer stockEsperado;
    private Integer stockContado;
    private Integer diferencia;
    private String observaciones;
    private Boolean ajusteAplicado;
}
