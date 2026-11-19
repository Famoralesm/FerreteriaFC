package com.ferreteria.fc.domain.service;

import com.ferreteria.fc.domain.dto.request.SupplierRequest;
import com.ferreteria.fc.persistence.entity.Supplier;
import java.util.List;

public interface SupplierService {
    void selectSupplier(Long productId, Long productSupplierId);
    Supplier create(SupplierRequest request);
    Supplier update(Long id, SupplierRequest request);
    Supplier get(Long id);
    List<Supplier> getAll();
}
