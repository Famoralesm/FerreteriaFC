package com.ferreteria.fc.domain.service;

import com.ferreteria.fc.domain.dto.request.ProductSupplierRequest;
import com.ferreteria.fc.persistence.entity.ProductSupplier;
import java.util.List;

public interface ProductSupplierService {
    ProductSupplier addSupplierToProduct(Long productId, ProductSupplierRequest request);
    ProductSupplier updateSupplierOffer(Long productId, Long offerId, ProductSupplierRequest request);
    List<ProductSupplier> getSuppliersByProduct(Long productId);
    List<ProductSupplier> getProductsBySupplier(Long supplierId);
}
