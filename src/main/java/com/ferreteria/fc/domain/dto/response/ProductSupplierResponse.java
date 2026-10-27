package com.ferreteria.fc.domain.dto.response;
import com.ferreteria.fc.persistence.entity.Product;
import com.ferreteria.fc.persistence.entity.Supplier;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class ProductSupplierResponse {

    private Long id;

    private Long supplierId;
    private Product product;

    private Supplier supplier;

    private String supplierPhone;

    private String supplierEmail;

    private BigDecimal purchasePrice;

    private Boolean selected;
}