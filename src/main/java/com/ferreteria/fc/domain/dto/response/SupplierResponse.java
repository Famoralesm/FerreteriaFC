package com.ferreteria.fc.domain.dto.response;

import lombok.*;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class SupplierResponse {
    private Long id;
    private String nombre;
    private String telefono;
    private String email;
}
