
package com.ferreteria.fc.domain.dto.response;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StoreResponse {

    private Long id;

    private String nombre;

    private String codigo;

    private String direccion;

    private Boolean activa;
}
