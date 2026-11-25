package com.ferreteria.fc.web.controller;

import com.ferreteria.fc.domain.dto.request.ProductUnitRequest;
import com.ferreteria.fc.domain.dto.response.ProductUnitResponse;
import com.ferreteria.fc.domain.enums.Role;
import com.ferreteria.fc.domain.service.ProductUnitService;
import com.ferreteria.fc.domain.service.UserService;
import com.ferreteria.fc.persistence.entity.ProductUnit;
import com.ferreteria.fc.persistence.entity.User;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/products/{productId}/units")
@CrossOrigin(origins = "http://localhost:4200")
public class ProductUnitController {

    private final ProductUnitService productUnitService;
    private final UserService userService;

    public ProductUnitController(
            ProductUnitService productUnitService,
            UserService userService
    ) {
        this.productUnitService = productUnitService;
        this.userService = userService;
    }

    @GetMapping
    public ResponseEntity<List<ProductUnitResponse>> listar(
            @PathVariable Long productId,
            Authentication authentication
    ) {
        validarAutenticado(authentication);
        return ResponseEntity.ok(productUnitService.getByProduct(productId)
                .stream().map(this::toResponse).toList());
    }

    @PostMapping
    public ResponseEntity<ProductUnitResponse> crear(
            @PathVariable Long productId,
            @Valid @RequestBody ProductUnitRequest request,
            Authentication authentication
    ) {
        validarAdmin(authentication);
        ProductUnit created = productUnitService.create(productId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(toResponse(created));
    }

    @PutMapping("/{unitId}")
    public ResponseEntity<ProductUnitResponse> actualizar(
            @PathVariable Long productId,
            @PathVariable Long unitId,
            @Valid @RequestBody ProductUnitRequest request,
            Authentication authentication
    ) {
        validarAdmin(authentication);
        return ResponseEntity.ok(toResponse(
                productUnitService.update(productId, unitId, request)
        ));
    }

    @PatchMapping("/{unitId}/deactivate")
    public ResponseEntity<Void> desactivar(
            @PathVariable Long productId,
            @PathVariable Long unitId,
            Authentication authentication
    ) {
        validarAdmin(authentication);
        productUnitService.deactivate(productId, unitId);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{unitId}")
    public ResponseEntity<?> eliminar(
            @PathVariable Long productId,
            @PathVariable Long unitId,
            Authentication authentication
    ) {
        validarAdmin(authentication);
        try {
            productUnitService.delete(productId, unitId);
            return ResponseEntity.noContent().build();
        } catch (IllegalStateException exception) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(Map.of("message", exception.getMessage()));
        }
    }

    @PatchMapping("/{unitId}/activate")
    public ResponseEntity<Void> activar(
            @PathVariable Long productId,
            @PathVariable Long unitId,
            Authentication authentication
    ) {
        validarAdmin(authentication);
        productUnitService.activate(productId, unitId);
        return ResponseEntity.noContent().build();
    }

    private void validarAutenticado(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new IllegalStateException("No existe un usuario autenticado");
        }
        userService.getByEmail(authentication.getName());
    }

    private void validarAdmin(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new IllegalStateException("No existe un usuario autenticado");
        }
        User user = userService.getByEmail(authentication.getName());
        if (user.getRole() != Role.ADMIN) {
            throw new org.springframework.security.access.AccessDeniedException(
                    "Solo un administrador puede administrar las presentaciones"
            );
        }
    }

    private BigDecimal resolveMinimumSaleQuantity(ProductUnit unit) {
        if (unit.getMinimumSaleQuantity() != null) {
            return unit.getMinimumSaleQuantity();
        }
        boolean baseUnit = unit.getProduct().getBaseUnit() != null
                && unit.getProduct().getBaseUnit().getCode().equals(unit.getCode());
        return baseUnit && unit.getProduct().getMinimumSaleQuantity() != null
                ? unit.getProduct().getMinimumSaleQuantity()
                : BigDecimal.ONE;
    }

    private BigDecimal resolveSaleQuantityIncrement(ProductUnit unit) {
        if (unit.getSaleQuantityIncrement() != null) {
            return unit.getSaleQuantityIncrement();
        }
        boolean baseUnit = unit.getProduct().getBaseUnit() != null
                && unit.getProduct().getBaseUnit().getCode().equals(unit.getCode());
        return baseUnit && unit.getProduct().getSaleQuantityIncrement() != null
                ? unit.getProduct().getSaleQuantityIncrement()
                : BigDecimal.ONE;
    }

    private ProductUnitResponse toResponse(ProductUnit unit) {
        return ProductUnitResponse.builder()
                .id(unit.getId())
                .productId(unit.getProduct().getId())
                .code(unit.getCode())
                .name(unit.getName())
                .conversionFactor(unit.getConversionFactor())
                .minimumSaleQuantity(resolveMinimumSaleQuantity(unit))
                .saleQuantityIncrement(resolveSaleQuantityIncrement(unit))
                .purchaseAllowed(unit.getPurchaseAllowed())
                .saleAllowed(unit.getSaleAllowed())
                .active(unit.getActive())
                .build();
    }
}