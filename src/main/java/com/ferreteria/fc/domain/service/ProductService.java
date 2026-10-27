package com.ferreteria.fc.domain.service;

import com.ferreteria.fc.domain.dto.request.CreateProductRequest;
import com.ferreteria.fc.domain.dto.request.PatchProductRequest;
import com.ferreteria.fc.domain.dto.request.UpdateProductRequest;
import com.ferreteria.fc.domain.dto.response.ProductResponse;
import com.ferreteria.fc.persistence.entity.Product;

import java.util.List;

public interface ProductService {

    ProductResponse createProduct(CreateProductRequest request);

    List<ProductResponse> getAll();

    ProductResponse getById(Long id);

    ProductResponse updateProduct(
            Long id,
            UpdateProductRequest request
    );

    ProductResponse patchProduct(
            Long id,
            PatchProductRequest request
    );

    void delete(Long id);
    List<Product> search(String query);
}