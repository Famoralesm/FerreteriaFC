package com.ferreteria.fc.web.controller;

import com.ferreteria.fc.domain.dto.mapper.ProductMapper;
import com.ferreteria.fc.domain.dto.request.CreateProductRequest;
import com.ferreteria.fc.domain.dto.request.PatchProductRequest;
import com.ferreteria.fc.domain.dto.request.UpdateProductRequest;
import com.ferreteria.fc.domain.dto.response.ProductResponse;
import com.ferreteria.fc.domain.service.ProductService;

import jakarta.validation.Valid;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/products")
@CrossOrigin(origins = "http://localhost:4200")
public class ProductController {

    private final ProductService productService;
    private final ProductMapper productMapper;


    public ProductController(
            ProductService productService, ProductMapper productMapper
    ) {
        this.productService = productService;
        this.productMapper = productMapper;
    }

    @PostMapping
    public ResponseEntity<ProductResponse> createProduct(
            @RequestBody @Valid CreateProductRequest request
    ) {

        return ResponseEntity.ok(
                productService.createProduct(request)
        );
    }

    @GetMapping
    public ResponseEntity<List<ProductResponse>> getAll() {

        return ResponseEntity.ok(
                productService.getAll()
        );
    }

    @GetMapping("/{id}")
    public ResponseEntity<ProductResponse> getById(
            @PathVariable Long id
    ) {

        return ResponseEntity.ok(
                productService.getById(id)
        );
    }

    @PutMapping("/{id}")
    public ResponseEntity<ProductResponse> update(
            @PathVariable Long id,
            @RequestBody @Valid UpdateProductRequest request
    ) {

        return ResponseEntity.ok(
                productService.updateProduct(id, request)
        );
    }

    @PatchMapping("/{id}")
    public ResponseEntity<ProductResponse> patch(
            @PathVariable Long id,
            @RequestBody PatchProductRequest request
    ) {

        return ResponseEntity.ok(
                productService.patchProduct(id, request)
        );
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(
            @PathVariable Long id
    ) {

        productService.delete(id);

        return ResponseEntity.noContent().build();
    }

    @GetMapping("/search")
    public ResponseEntity<List<ProductResponse>> search(
            @RequestParam String query) {

        return ResponseEntity.ok(
                productService.search(query)
                        .stream()
                        .map(productMapper::toResponse)
                        .toList()
        );
    }
}