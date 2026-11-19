package com.ferreteria.fc.domain.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class SupplierRequest {
    @NotBlank
    private String nombre;
    private String telefono;
    @Email
    private String email;
}
