package com.ferreteria.fc.web.controller;

import com.ferreteria.fc.domain.dto.mapper.ProductSupplierMapper;
import com.ferreteria.fc.domain.dto.request.ProductSupplierRequest;
import com.ferreteria.fc.domain.dto.request.SupplierRequest;
import com.ferreteria.fc.domain.dto.response.ProductSupplierResponse;
import com.ferreteria.fc.domain.dto.response.SupplierResponse;
import com.ferreteria.fc.domain.service.ProductSupplierService;
import com.ferreteria.fc.domain.service.SupplierService;
import com.ferreteria.fc.persistence.entity.ProductSupplier;
import com.ferreteria.fc.persistence.entity.Supplier;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/suppliers")
@CrossOrigin(origins = "http://localhost:4200")
public class SupplierController {
    private final ProductSupplierService productSupplierService;
    private final SupplierService supplierService;
    private final ProductSupplierMapper mapper;

    public SupplierController(ProductSupplierService productSupplierService,
            SupplierService supplierService, ProductSupplierMapper mapper) {
        this.productSupplierService = productSupplierService;
        this.supplierService = supplierService;
        this.mapper = mapper;
    }

    @GetMapping
    public ResponseEntity<List<SupplierResponse>> listar() {
        return ResponseEntity.ok(supplierService.getAll().stream().map(this::supplierResponse).toList());
    }

    @PostMapping
    public ResponseEntity<SupplierResponse> crear(@Valid @RequestBody SupplierRequest request) {
        return ResponseEntity.ok(supplierResponse(supplierService.create(request)));
    }

    @GetMapping("/{supplierId}")
    public ResponseEntity<SupplierResponse> obtener(@PathVariable Long supplierId) {
        return ResponseEntity.ok(supplierResponse(supplierService.get(supplierId)));
    }

    @PutMapping("/{supplierId}")
    public ResponseEntity<SupplierResponse> actualizar(@PathVariable Long supplierId,
            @Valid @RequestBody SupplierRequest request) {
        return ResponseEntity.ok(supplierResponse(supplierService.update(supplierId, request)));
    }

    @GetMapping("/products/{productId}")
    public ResponseEntity<List<ProductSupplierResponse>> listarProveedoresProducto(@PathVariable Long productId) {
        return ResponseEntity.ok(mapper.toListResponse(productSupplierService.getSuppliersByProduct(productId)));
    }

    @GetMapping("/{supplierId}/products")
    public ResponseEntity<List<ProductSupplierResponse>> listarProductosProveedor(@PathVariable Long supplierId) {
        return ResponseEntity.ok(mapper.toListResponse(productSupplierService.getProductsBySupplier(supplierId)));
    }

    @PostMapping({"/{productId}/", "/products/{productId}"})
    public ResponseEntity<ProductSupplierResponse> asociar(@PathVariable Long productId,
            @Valid @RequestBody ProductSupplierRequest request) {
        ProductSupplier result = productSupplierService.addSupplierToProduct(productId, request);
        return ResponseEntity.ok(mapper.toResponse(result));
    }

    @PutMapping("/{productId}/suppliers/{productSupplierId}/select")
    public ResponseEntity<Void> seleccionar(@PathVariable Long productId, @PathVariable Long productSupplierId) {
        supplierService.selectSupplier(productId, productSupplierId);
        return ResponseEntity.noContent().build();
    }

    private SupplierResponse supplierResponse(Supplier s) {
        return SupplierResponse.builder().id(s.getId()).rut(s.getRut()).nombre(s.getNombre())
                .razonSocial(s.getRazonSocial()).giro(s.getGiro()).direccion(s.getDireccion())
                .comuna(s.getComuna()).ciudad(s.getCiudad()).telefono(s.getTelefono())
                .email(s.getEmail()).activo(s.getActivo()).createdAt(s.getCreatedAt())
                .updatedAt(s.getUpdatedAt()).build();
    }
}
