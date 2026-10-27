package com.ferreteria.fc.domain.service;

import com.ferreteria.fc.domain.dto.request.ProductSupplierRequest;
import com.ferreteria.fc.persistence.entity.ProductSupplier;

public interface ProductSupplierService {

    public ProductSupplier addSupplierToProduct(
            Long productId,
            ProductSupplierRequest request);
}
