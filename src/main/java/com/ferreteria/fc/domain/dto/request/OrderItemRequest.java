package com.ferreteria.fc.domain.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class OrderItemRequest {
    private Long productId;
    private String productName;
    @NotNull
    @DecimalMin(value = "0.0001", message = "La cantidad debe ser mayor que cero")
    private BigDecimal quantity;
    @DecimalMin("1")
    private BigDecimal unitPrice;
}