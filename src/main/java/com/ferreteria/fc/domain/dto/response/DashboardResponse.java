package com.ferreteria.fc.domain.dto.response;

import java.math.BigDecimal;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DashboardResponse {

    private BigDecimal ventasHoy;

    private Long pedidosPendientes;

    private Long clientes;

    private Long productos;

}