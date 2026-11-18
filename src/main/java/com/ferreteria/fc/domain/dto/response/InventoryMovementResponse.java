
package com.ferreteria.fc.domain.dto.response;

import com.ferreteria.fc.domain.enums.MovementType;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InventoryMovementResponse {

    private Long id;

    private Long productoId;

    private String sku;

    private String nombreProducto;

    private MovementType tipoMovimiento;

    private Integer cantidad;

    private Integer stockAnterior;

    private Integer stockNuevo;

    private String motivo;

    private String tipoReferencia;

    private Long referenciaId;

    private String usuario;

    private LocalDateTime fechaCreacion;

    private Long sucursalId;

    private String nombreSucursal;
}
