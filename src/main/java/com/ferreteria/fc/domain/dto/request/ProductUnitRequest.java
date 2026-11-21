package com.ferreteria.fc.domain.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class ProductUnitRequest {

    @NotBlank
    @Size(max = 30)
    private String code;

    @NotBlank
    @Size(max = 80)
    private String name;

    @NotNull
    @DecimalMin(value = "0", inclusive = false)
    private BigDecimal conversionFactor;

    @NotNull
    private Boolean purchaseAllowed;

    @NotNull
    private Boolean saleAllowed;
}