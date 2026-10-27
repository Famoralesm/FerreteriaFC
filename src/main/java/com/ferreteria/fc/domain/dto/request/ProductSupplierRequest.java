package com.ferreteria.fc.domain.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.DecimalMin;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class ProductSupplierRequest {

    @NotNull
    private Long supplierId;

    @NotNull
    @DecimalMin("0.0")
    private BigDecimal purchasePrice;

    private Boolean selected = false;
}