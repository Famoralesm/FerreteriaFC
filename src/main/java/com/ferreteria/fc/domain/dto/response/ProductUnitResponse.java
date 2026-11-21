package com.ferreteria.fc.domain.dto.response;

import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductUnitResponse {
    private Long id;
    private Long productId;
    private String code;
    private String name;
    private BigDecimal conversionFactor;
    private Boolean purchaseAllowed;
    private Boolean saleAllowed;
    private Boolean active;
}