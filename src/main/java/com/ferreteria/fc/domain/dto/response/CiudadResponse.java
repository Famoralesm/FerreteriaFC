package com.ferreteria.fc.domain.dto.response;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CiudadResponse {
    Long id;
    String nombre;
}
