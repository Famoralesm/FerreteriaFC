package com.ferreteria.fc.domain.dto.response;

import com.ferreteria.fc.persistence.entity.Ciudad;
import com.ferreteria.fc.persistence.entity.Comuna;
import lombok.*;
import java.time.LocalDateTime;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class SupplierResponse {
    private Long id;
    private String rut;
    private String nombre;
    private String razonSocial;
    private String giro;
    private String direccion;
    private Comuna comuna;
    private Ciudad ciudad;
    private String telefono;
    private String email;
    private Boolean activo;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
