package com.ferreteria.fc.domain.dto.mapper;

import com.ferreteria.fc.domain.dto.response.ProductResponse;
import com.ferreteria.fc.persistence.entity.Product;
import org.springframework.stereotype.Component;

import java.util.List;
import java.math.BigDecimal;

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
                .wholesalePrice(product.getWholesalePrice())
                .wholesaleMinQuantity(product.getWholesaleMinQuantity())
                .minimumSaleQuantity(product.getMinimumSaleQuantity())
                .saleQuantityIncrement(product.getSaleQuantityIncrement())
                .baseUnitId(product.getBaseUnit() != null ? product.getBaseUnit().getId() : null)
                .baseUnitCode(product.getBaseUnit() != null ? product.getBaseUnit().getCode() : null)
                .baseUnitName(product.getBaseUnit() != null ? product.getBaseUnit().getName() : null)
                .baseUnitSymbol(product.getBaseUnit() != null ? product.getBaseUnit().getSymbol() : null)
                .stock(product.getStock() == null ? BigDecimal.ZERO : BigDecimal.valueOf(product.getStock()))
                .active(product.getActive())
                .category(product.getCategory())
                .brand(product.getBrand())
                .unit(product.getUnit())
                .createdAt(product.getCreatedAt())
                .updatedAt(product.getUpdatedAt())
                .imageUrl(
                        product.getImagePath() == null
                                ? null
                                : "/api/media/" + product.getImagePath()
                )
                .build();
    }

    public List<ProductResponse> toListResponse(List<Product> products) {

        return products.stream()
                .map(this::toResponse)
                .toList();
    }
}
