package com.ferreteria.fc.domain.service;

import com.ferreteria.fc.domain.dto.request.ProductUnitRequest;
import com.ferreteria.fc.persistence.entity.ProductUnit;

import java.util.List;

public interface ProductUnitService {
    List<ProductUnit> getByProduct(Long productId);
    ProductUnit create(Long productId, ProductUnitRequest request);
    ProductUnit update(Long productId, Long unitId, ProductUnitRequest request);
    void deactivate(Long productId, Long unitId);
}