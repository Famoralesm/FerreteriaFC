package com.ferreteria.fc.domain.dto.mapper;

import com.ferreteria.fc.domain.dto.response.ProductSupplierResponse;
import com.ferreteria.fc.persistence.entity.ProductSupplier;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class ProductSupplierMapper {

    public ProductSupplierResponse toResponse(ProductSupplier productSupplier) {

        if (productSupplier == null) {
            return null;
        }

        return ProductSupplierResponse.builder()
                .id(productSupplier.getId())
                .product(productSupplier.getProduct())
                .supplier(productSupplier.getSupplier())
                .purchasePrice(productSupplier.getPurchasePrice())
                .selected(productSupplier.getSelected())
                .build();
    }

    public List<ProductSupplierResponse> toListResponse(List<ProductSupplier> productSupplier) {

        return productSupplier.stream()
                .map(this::toResponse)
                .toList();
    }


}
