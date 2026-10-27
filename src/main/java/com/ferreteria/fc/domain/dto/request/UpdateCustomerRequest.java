package com.ferreteria.fc.domain.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdateCustomerRequest{

        @NotBlank
        String rut;

        @NotBlank
        String nombre;

        String razonSocial;

        String giro;

        String tipoCompra;

        String direccion;

        String comuna;

        String ciudad;

        String contacto;

        @Email
        String email;

        String telefono;

        String rutSolicita;

        Boolean activo;

}