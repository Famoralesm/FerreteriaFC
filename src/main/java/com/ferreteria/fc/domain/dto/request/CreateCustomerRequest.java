package com.ferreteria.fc.domain.dto.request;

import com.ferreteria.fc.persistence.entity.Ciudad;
import com.ferreteria.fc.persistence.entity.Comuna;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateCustomerRequest{

    @NotBlank
    String rut;

    @NotBlank
    String nombre;

    String razonSocial;

    String giro;

    String direccion;

    Comuna comuna;

    Ciudad ciudad;


    @Email
    String email;

    String telefono;


    Boolean activo;
}


