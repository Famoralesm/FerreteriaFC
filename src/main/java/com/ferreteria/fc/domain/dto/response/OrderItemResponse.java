package com.ferreteria.fc.domain.dto.response;

import lombok.*;
import java.math.BigDecimal;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderItemResponse {
    private Long id;
    private Long productId;
    private String productName;
    private BigDecimal quantity;
    private Long unitId;
    private String unitCode;
    private String unitName;
    private BigDecimal conversionFactor;
    private BigDecimal quantityInBaseUnit;
    private BigDecimal unitPrice;
    private BigDecimal totalPrice;
    private Boolean wholesaleApplied;
    private String pricingType;
}
