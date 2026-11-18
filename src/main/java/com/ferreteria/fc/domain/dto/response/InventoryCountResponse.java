package com.ferreteria.fc.domain.dto.response;

import com.ferreteria.fc.domain.enums.InventoryCountStatus;
import lombok.*;
import java.time.LocalDateTime;
import java.util.List;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class InventoryCountResponse {
    private Long id;
    private String descripcion;
    private InventoryCountStatus estado;
    private Long sucursalId;
    private String nombreSucursal;
    private LocalDateTime fechaCreacion;
    private LocalDateTime iniciadoEn;
    private LocalDateTime completadoEn;
    private List<InventoryCountDetailResponse> detalles;
}
