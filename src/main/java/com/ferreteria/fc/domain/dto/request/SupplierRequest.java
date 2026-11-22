package com.ferreteria.fc.domain.dto.request;

import com.ferreteria.fc.persistence.entity.Ciudad;
import com.ferreteria.fc.persistence.entity.Comuna;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.*;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class SupplierRequest {
    @NotBlank
    @Pattern(regexp = "^[0-9]{7,8}[0-9K]$", message = "El RUT debe tener un formato válido.")
    private String rut;

    @NotBlank
    private String nombre;

    @NotBlank
    private String razonSocial;

    @NotBlank
    private String giro;

    private String direccion;
    private Comuna comuna;
    private Ciudad ciudad;

    @NotBlank
    @Pattern(regexp = "^(?:\\+?56)?[2-9][0-9]{8}$", message = "El teléfono debe tener un formato chileno válido.")
    private String telefono;

    @Email
    private String email;

    private Boolean activo;
}
