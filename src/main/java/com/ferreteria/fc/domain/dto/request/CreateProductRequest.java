package com.ferreteria.fc.domain.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateProductRequest {

    @NotBlank
    private String sku;

    @NotBlank
    private String name;

    private String description;

    @NotNull
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

    private Boolean active;

    private String category;

    private String brand;

    private Long brandId;

    private String unit;
}