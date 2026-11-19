package com.ferreteria.fc.domain.dto.request;

import com.ferreteria.fc.persistence.entity.Ciudad;
import com.ferreteria.fc.persistence.entity.Comuna;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class SupplierRequest {
    private String rut;
    @NotBlank
    private String nombre;
    private String razonSocial;
    private String giro;
    private String direccion;
    private Comuna comuna;
    private Ciudad ciudad;
    private String telefono;
    @Email
    private String email;
    private Boolean activo;
}
