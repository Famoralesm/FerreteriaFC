
package com.ferreteria.fc.domain.dto.response;

import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductStockResponse {

    private Long id;

    private Long productoId;

    private String sku;

    private String nombreProducto;

    private String categoria;

    private String marca;

    private Long sucursalId;

    private String nombreSucursal;

    private Integer stock;

    private Integer stockMinimo;

    private Boolean bajoStock;

    private LocalDateTime fechaActualizacion;
}
