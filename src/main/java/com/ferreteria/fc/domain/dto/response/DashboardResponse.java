package com.ferreteria.fc.domain.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DashboardResponse {

    private Double ventasHoy;

    private Long pedidosPendientes;

    private Long clientes;

    private Long productos;

}