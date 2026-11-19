package com.ferreteria.fc.domain.dto.response;

import lombok.*;
import java.math.BigDecimal;

@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class ProductSupplierResponse {
    private Long id;
    private Long productoId;
    private String nombreProducto;
    private Long proveedorId;
    private String nombreProveedor;
    private String telefonoProveedor;
    private String emailProveedor;
    private BigDecimal precioCompra;
    private Boolean seleccionado;
}
