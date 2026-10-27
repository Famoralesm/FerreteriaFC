package com.ferreteria.fc.web.controller;

import com.ferreteria.fc.domain.dto.mapper.CiudadMapper;
import com.ferreteria.fc.domain.dto.mapper.ProductSupplierMapper;
import com.ferreteria.fc.domain.dto.request.ProductSupplierRequest;
import com.ferreteria.fc.domain.dto.response.ProductSupplierResponse;
import com.ferreteria.fc.domain.service.ProductSupplierService;
import com.ferreteria.fc.domain.service.SupplierService;
import com.ferreteria.fc.persistence.entity.ProductSupplier;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/suppliers")
public class SupplierController {

    private final ProductSupplierService productSupplierService;
    private final SupplierService supplierService;
    private final ProductSupplierMapper productSupplierMapper;

    public SupplierController(
            ProductSupplierService productSupplierService, SupplierService supplierService, ProductSupplierMapper productSupplierMapper
    ) {
        this.productSupplierService = productSupplierService;
        this.supplierService = supplierService;
        this.productSupplierMapper = productSupplierMapper;
    }


    @PostMapping("/{productId}/")
    public ResponseEntity<ProductSupplierResponse> addSupplier(
            @PathVariable Long productId,
            @RequestBody ProductSupplierRequest request) {

        ProductSupplier result =
                productSupplierService.addSupplierToProduct(
                        productId,
                        request
                );

        return ResponseEntity.ok(
                productSupplierMapper.toResponse(result)
        );
    }

    @PutMapping("/{productId}/suppliers/{productSupplierId}/select")
    public ResponseEntity<Void> selectSupplier(
            @PathVariable Long productId,
            @PathVariable Long productSupplierId) {

        supplierService.selectSupplier(
                productId,
                productSupplierId
        );

        return ResponseEntity.noContent().build();
    }
}
