package com.ferreteria.fc.domain.dto.response;

import com.ferreteria.fc.domain.enums.OrderStatus;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderSummaryResponse {

    private Long id;
    private String cliente;
    private LocalDateTime fecha;
    private BigDecimal total;
    private OrderStatus estado;
    private LocalDateTime statusChangedAt;
}
