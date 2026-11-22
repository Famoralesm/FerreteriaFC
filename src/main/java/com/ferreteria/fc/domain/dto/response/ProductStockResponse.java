
package com.ferreteria.fc.domain.dto.response;

import lombok.*;

import java.time.LocalDateTime;
import java.math.BigDecimal;

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

    private BigDecimal stock;

    private BigDecimal stockMinimo;

    private Boolean bajoStock;

    private LocalDateTime fechaActualizacion;

    private Boolean inventarioInicialRegistrado;
}
