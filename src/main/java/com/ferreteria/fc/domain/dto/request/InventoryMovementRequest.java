
package com.ferreteria.fc.domain.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InventoryMovementRequest {

    @NotNull
    private Long productoId;

    @NotNull
    private Integer cantidad;

    private String motivo;

    private String tipoReferencia;

    private Long referenciaId;
}
