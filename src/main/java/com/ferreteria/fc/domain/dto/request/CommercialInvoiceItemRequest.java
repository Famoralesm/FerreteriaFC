package com.ferreteria.fc.domain.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;
import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CommercialInvoiceItemRequest {
    private Long productId;

    @NotBlank
    @Size(max = 200)
    private String productName;

    @NotNull
    @DecimalMin(value = "0.0001")
    private BigDecimal quantity;

    @Size(max = 80)
    private String unitName;

    @NotNull
    @DecimalMin(value = "0.00")
    private BigDecimal unitPrice;
}
