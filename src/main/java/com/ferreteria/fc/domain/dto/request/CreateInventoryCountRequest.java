package com.ferreteria.fc.domain.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class CreateInventoryCountRequest {
    @NotBlank
    private String descripcion;
    private Long sucursalId;
}
