package com.ferreteria.fc.domain.service.Impl;

import com.ferreteria.fc.domain.dto.request.ProductUnitRequest;
import com.ferreteria.fc.domain.repository.ProductRepository;
import com.ferreteria.fc.domain.repository.ProductUnitRepository;
import com.ferreteria.fc.domain.service.ProductUnitService;
import com.ferreteria.fc.domain.service.AuditLogService;
import com.ferreteria.fc.persistence.entity.Product;
import com.ferreteria.fc.persistence.entity.ProductUnit;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.LinkedHashMap;

@Service
@Transactional
public class ProductUnitServiceImpl implements ProductUnitService {

    private final ProductRepository productRepository;
    private final ProductUnitRepository productUnitRepository;
    private final AuditLogService auditLogService;

    public ProductUnitServiceImpl(
            ProductRepository productRepository,
            ProductUnitRepository productUnitRepository,
            AuditLogService auditLogService
    ) {
        this.productRepository = productRepository;
        this.productUnitRepository = productUnitRepository;
        this.auditLogService = auditLogService;
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

        ProductUnit saved = productUnitRepository.save(ProductUnit.builder()
                .product(product)
                .code(code)
                .name(request.getName().trim())
                .conversionFactor(request.getConversionFactor())
                .minimumSaleQuantity(request.getMinimumSaleQuantity())
                .saleQuantityIncrement(request.getSaleQuantityIncrement())
                .purchaseAllowed(request.getPurchaseAllowed())
                .saleAllowed(request.getSaleAllowed())
                .active(true)
                .build());
        recordPresentationChange(saved, "CREATED", null, "Presentación de producto creada");
        return saved;
    }

    @Override
    public ProductUnit update(Long productId, Long unitId, ProductUnitRequest request) {
        ProductUnit unit = obtenerUnidad(productId, unitId);
        Map<String, Object> previousValues = presentationAuditValues(unit);
        String code = normalizarCodigo(request.getCode());

        productUnitRepository.findByProductIdAndCode(productId, code)
                .filter(existing -> !existing.getId().equals(unitId))
                .ifPresent(existing -> {
                    throw new IllegalStateException("Ya existe una presentación con código " + code);
                });

        if (unit.getProduct().getBaseUnit() != null
                && unit.getProduct().getBaseUnit().getCode().equals(unit.getCode())
                && (!code.equals(unit.getProduct().getBaseUnit().getCode())
                || request.getConversionFactor().compareTo(java.math.BigDecimal.ONE) != 0)) {
            throw new IllegalStateException("La presentación correspondiente a la unidad base debe conservar su código y factor 1.");
        }
        unit.setCode(code);
        unit.setName(request.getName().trim());
        unit.setConversionFactor(request.getConversionFactor());
        unit.setMinimumSaleQuantity(request.getMinimumSaleQuantity());
        unit.setSaleQuantityIncrement(request.getSaleQuantityIncrement());
        unit.setPurchaseAllowed(request.getPurchaseAllowed());
        unit.setSaleAllowed(request.getSaleAllowed());

        ProductUnit saved = productUnitRepository.save(unit);
        recordPresentationChange(saved, "UPDATED", previousValues, "Presentación de producto actualizada");
        return saved;
    }

    @Override
    public void deactivate(Long productId, Long unitId) {
        ProductUnit unit = obtenerUnidad(productId, unitId);
        if (esUnidadBase(unit)) {
            throw new IllegalStateException("No se puede desactivar la presentación que representa la unidad base del producto.");
        }
        Map<String, Object> previousValues = presentationAuditValues(unit);
        unit.setActive(false);
        ProductUnit saved = productUnitRepository.save(unit);
        recordPresentationChange(saved, "DEACTIVATED", previousValues, "Presentación de producto desactivada");
    }

    @Override
    public void activate(Long productId, Long unitId) {
        ProductUnit unit = obtenerUnidad(productId, unitId);
        Map<String, Object> previousValues = presentationAuditValues(unit);
        unit.setActive(true);
        ProductUnit saved = productUnitRepository.save(unit);
        recordPresentationChange(saved, "ACTIVATED", previousValues, "Presentación de producto activada");
    }

    @Override
    public void delete(Long productId, Long unitId) {
        ProductUnit unit = obtenerUnidad(productId, unitId);
        if (esUnidadBase(unit)) {
            throw new IllegalStateException("No se puede eliminar la presentación que representa la unidad base del producto.");
        }
        if (productUnitRepository.countOrderItemSnapshotsByUnitId(unitId) > 0) {
            throw new IllegalStateException("No se puede eliminar esta presentación porque ya forma parte de pedidos históricos. Puedes desactivarla para impedir nuevas ventas.");
        }
        auditLogService.recordForCurrentUser(
                "PRODUCTS", "PRODUCT_UNIT", unit.getId().toString(), "DELETED", null,
                presentationAuditValues(unit), Map.of("deleted", true),
                "Presentación de producto eliminada", "WEB"
        );
        productUnitRepository.delete(unit);
    }

    private Map<String, Object> presentationAuditValues(ProductUnit unit) {
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("productId", unit.getProduct().getId());
        values.put("code", unit.getCode());
        values.put("name", unit.getName());
        values.put("conversionFactor", unit.getConversionFactor());
        values.put("minimumSaleQuantity", unit.getMinimumSaleQuantity());
        values.put("saleQuantityIncrement", unit.getSaleQuantityIncrement());
        values.put("purchaseAllowed", unit.getPurchaseAllowed());
        values.put("saleAllowed", unit.getSaleAllowed());
        values.put("active", unit.getActive());
        return values;
    }

    private void recordPresentationChange(
            ProductUnit unit,
            String action,
            Map<String, Object> previousValues,
            String description
    ) {
        auditLogService.recordForCurrentUser(
                "PRODUCTS", "PRODUCT_UNIT", unit.getId().toString(), action, null,
                previousValues, presentationAuditValues(unit), description, "WEB"
        );
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

    private boolean esUnidadBase(ProductUnit unit) {
        return unit.getProduct().getBaseUnit() != null
                && unit.getProduct().getBaseUnit().getCode().equals(unit.getCode());
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