package com.ferreteria.fc.domain.dto.response;

import com.ferreteria.fc.domain.enums.CommercialInvoiceStatus;
import com.ferreteria.fc.domain.enums.InvoiceTaxStatus;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record CommercialInvoiceResponse(
        Long id,
        String internalNumber,
        Long storeId,
        String storeName,
        Long sourceOrderId,
        String sourceOrderNumber,
        String customerName,
        String customerRut,
        String customerBusinessName,
        String customerGiro,
        String customerAddress,
        String customerCity,
        String customerCommune,
        String customerEmail,
        String customerPhone,
        BigDecimal subtotal,
        BigDecimal totalAmount,
        CommercialInvoiceStatus status,
        InvoiceTaxStatus taxStatus,
        String createdByName,
        String notes,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        LocalDateTime canceledAt,
        List<CommercialInvoiceItemResponse> items
) {}
