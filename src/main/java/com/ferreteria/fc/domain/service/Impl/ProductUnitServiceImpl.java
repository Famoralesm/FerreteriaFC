package com.ferreteria.fc.domain.service.Impl;

import com.ferreteria.fc.domain.dto.request.ProductUnitRequest;
import com.ferreteria.fc.domain.repository.ProductRepository;
import com.ferreteria.fc.domain.repository.ProductUnitRepository;
import com.ferreteria.fc.domain.service.ProductUnitService;
import com.ferreteria.fc.persistence.entity.Product;
import com.ferreteria.fc.persistence.entity.ProductUnit;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;

@Service
@Transactional
public class ProductUnitServiceImpl implements ProductUnitService {

    private final ProductRepository productRepository;
    private final ProductUnitRepository productUnitRepository;

    public ProductUnitServiceImpl(
            ProductRepository productRepository,
            ProductUnitRepository productUnitRepository
    ) {
        this.productRepository = productRepository;
        this.productUnitRepository = productUnitRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProductUnit> getByProduct(Long productId) {
        validarProducto(productId);
        return productUnitRepository.findByProductIdOrderByNameAsc(productId);
    }

    @Override
    public ProductUnit create(Long productId, ProductUnitRequest request) {
        Product product = obtenerProducto(productId);
        String code = normalizarCodigo(request.getCode());

        if (productUnitRepository.existsByProductIdAndCode(productId, code)) {
            throw new IllegalStateException("Ya existe una presentación con código " + code);
        }

        return productUnitRepository.save(ProductUnit.builder()
                .product(product)
                .code(code)
                .name(request.getName().trim())
                .conversionFactor(request.getConversionFactor())
                .purchaseAllowed(request.getPurchaseAllowed())
                .saleAllowed(request.getSaleAllowed())
                .active(true)
                .build());
    }

    @Override
    public ProductUnit update(Long productId, Long unitId, ProductUnitRequest request) {
        ProductUnit unit = obtenerUnidad(productId, unitId);
        String code = normalizarCodigo(request.getCode());

        productUnitRepository.findByProductIdAndCode(productId, code)
                .filter(existing -> !existing.getId().equals(unitId))
                .ifPresent(existing -> {
                    throw new IllegalStateException("Ya existe una presentación con código " + code);
                });

        unit.setCode(code);
        unit.setName(request.getName().trim());
        unit.setConversionFactor(request.getConversionFactor());
        unit.setPurchaseAllowed(request.getPurchaseAllowed());
        unit.setSaleAllowed(request.getSaleAllowed());

        return productUnitRepository.save(unit);
    }

    @Override
    public void deactivate(Long productId, Long unitId) {
        ProductUnit unit = obtenerUnidad(productId, unitId);
        unit.setActive(false);
        productUnitRepository.save(unit);
    }

    private Product obtenerProducto(Long productId) {
        return productRepository.findById(productId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Producto no encontrado: " + productId
                ));
    }

    private void validarProducto(Long productId) {
        if (!productRepository.existsById(productId)) {
            throw new IllegalArgumentException("Producto no encontrado: " + productId);
        }
    }

    private ProductUnit obtenerUnidad(Long productId, Long unitId) {
        return productUnitRepository.findByIdAndProductId(unitId, productId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "La presentación no existe para el producto indicado"
                ));
    }

    private String normalizarCodigo(String code) {
        return code.trim().toUpperCase(Locale.ROOT);
    }
}