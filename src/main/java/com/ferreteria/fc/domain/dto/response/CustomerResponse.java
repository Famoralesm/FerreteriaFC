package com.ferreteria.fc.domain.dto.response;

import com.ferreteria.fc.persistence.entity.Ciudad;
import com.ferreteria.fc.persistence.entity.Comuna;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CustomerResponse {

    Long id;

    String rut;

    String nombre;

    String razonSocial;

    String giro;

    String direccion;

    Comuna comuna;

    Ciudad ciudad;

    String email;

    String telefono;

    Boolean activo;

    LocalDateTime createdAt;

    LocalDateTime updatedAt;
}
