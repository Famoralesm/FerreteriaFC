package com.ferreteria.fc.domain.service;

import com.ferreteria.fc.domain.dto.request.CreateProductRequest;
import com.ferreteria.fc.domain.dto.request.PatchProductRequest;
import com.ferreteria.fc.domain.dto.request.UpdateProductRequest;
import com.ferreteria.fc.domain.dto.response.ProductResponse;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface ProductService {

    ProductResponse createProduct(CreateProductRequest request);

    List<ProductResponse> getAll(Long sucursalId);

    ProductResponse getById(Long id, Long sucursalId);

    ProductResponse updateProduct(
            Long id,
            UpdateProductRequest request
    );

    ProductResponse patchProduct(
            Long id,
            PatchProductRequest request
    );

    ProductResponse setBaseUnit(Long id, Long unitOfMeasureId);

    void delete(Long id);

    List<ProductResponse> search(
            String query,
            Long sucursalId
    );

    ProductResponse updateProductImage(
            Long id,
            MultipartFile file
    );

    ProductResponse deleteProductImage(Long id);
}