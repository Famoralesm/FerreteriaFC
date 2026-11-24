package com.ferreteria.fc.domain.dto.request;

import jakarta.validation.constraints.DecimalMin;
import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PatchProductRequest {

    private String sku;

    private String name;

    private String description;

    @DecimalMin(value = "0.0", inclusive = true)
    private BigDecimal price;

    @DecimalMin(value = "0.0", inclusive = true)
    private BigDecimal wholesalePrice;

    @DecimalMin(value = "0.0001")
    private BigDecimal wholesaleMinQuantity;

    @DecimalMin(value = "0.0001")
    private BigDecimal minimumSaleQuantity;

    @DecimalMin(value = "0.0001")
    private BigDecimal saleQuantityIncrement;

    @jakarta.validation.constraints.Min(0)
    private Integer stock;

    private Boolean active;

    private String category;

    private String brand;

    private String unit;
}