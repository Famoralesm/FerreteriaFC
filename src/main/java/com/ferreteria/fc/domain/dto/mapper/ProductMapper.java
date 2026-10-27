package com.ferreteria.fc.domain.dto.mapper;

import com.ferreteria.fc.domain.dto.response.ProductResponse;
import com.ferreteria.fc.persistence.entity.Product;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class ProductMapper {

    public ProductResponse toResponse(Product product) {

        if (product == null) {
            return null;
        }

        return ProductResponse.builder()
                .id(product.getId())
                .sku(product.getSku())
                .name(product.getName())
                .description(product.getDescription())
                .price(product.getPrice())
                .stock(product.getStock())
                .active(product.getActive())
                .category(product.getCategory())
                .brand(product.getBrand())
                .unit(product.getUnit())
                .createdAt(product.getCreatedAt())
                .updatedAt(product.getUpdatedAt())
                .build();
    }

    public List<ProductResponse> toListResponse(List<Product> products) {

        return products.stream()
                .map(this::toResponse)
                .toList();
    }
}