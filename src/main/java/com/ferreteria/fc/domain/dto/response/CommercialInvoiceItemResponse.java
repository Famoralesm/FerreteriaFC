package com.ferreteria.fc.domain.dto.response;

import java.math.BigDecimal;

public record CommercialInvoiceItemResponse(
        Long id,
        Long productId,
        String productName,
        BigDecimal quantity,
        String unitName,
        BigDecimal unitPrice,
        BigDecimal lineTotal
) {}
