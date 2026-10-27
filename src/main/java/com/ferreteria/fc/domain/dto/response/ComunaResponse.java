package com.ferreteria.fc.domain.dto.response;


import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ComunaResponse {

    Long id;
    String nombre;
    Long id_ciudad;
}
