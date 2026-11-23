package com.ferreteria.fc.domain.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class DashboardProductStatusResponse {

    // Categorías excluyentes para que el gráfico represente el total sin duplicar productos.
    private Long active;
    private Long inactive;
    private Long lowStock;
    private Long total;
}
