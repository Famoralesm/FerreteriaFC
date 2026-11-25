package com.ferreteria.fc.domain.service.Impl;

import com.ferreteria.fc.domain.dto.request.ProductSupplierRequest;
import com.ferreteria.fc.domain.repository.BrandRepository;
import com.ferreteria.fc.domain.repository.ProductRepository;
import com.ferreteria.fc.domain.repository.ProductSupplierRepository;
import com.ferreteria.fc.domain.repository.ProductUnitRepository;
import com.ferreteria.fc.domain.repository.SupplierRepository;
import com.ferreteria.fc.domain.service.ProductSupplierService;
import com.ferreteria.fc.persistence.entity.Brand;
import com.ferreteria.fc.persistence.entity.Product;
import com.ferreteria.fc.persistence.entity.ProductSupplier;
import com.ferreteria.fc.persistence.entity.ProductUnit;
import com.ferreteria.fc.persistence.entity.Supplier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class ProductSupplierServiceImpl implements ProductSupplierService {
    private final ProductRepository productRepository;
    private final SupplierRepository supplierRepository;
    private final ProductSupplierRepository productSupplierRepository;
    private final BrandRepository brandRepository;
    private final ProductUnitRepository productUnitRepository;

    public ProductSupplierServiceImpl(ProductRepository productRepository,
            SupplierRepository supplierRepository,
            ProductSupplierRepository productSupplierRepository,
            BrandRepository brandRepository,
            ProductUnitRepository productUnitRepository) {
        this.productRepository = productRepository;
        this.supplierRepository = supplierRepository;
        this.productSupplierRepository = productSupplierRepository;
        this.brandRepository = brandRepository;
        this.productUnitRepository = productUnitRepository;
    }

    @Override
    public ProductSupplier addSupplierToProduct(Long productId, ProductSupplierRequest request) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new IllegalArgumentException("Producto no encontrado"));
        Supplier supplier = supplierRepository.findById(request.getSupplierId())
                .orElseThrow(() -> new IllegalArgumentException("Proveedor no encontrado"));
        Brand brand = brandRepository.findByIdAndActiveTrue(request.getBrandId())
                .orElseThrow(() -> new IllegalArgumentException("La marca no existe o está inactiva."));
        ProductUnit unit = resolvePurchaseUnit(productId, request.getProductUnitId());

        if (!Boolean.TRUE.equals(supplier.getActivo())) {
            throw new IllegalStateException("No se pueden registrar ofertas de un proveedor inactivo.");
        }
        validatePrice(request);
        ensureUniqueOffer(productId, request, null);

        List<ProductSupplier> current = productSupplierRepository.findByProductId(productId);
        boolean selected = Boolean.TRUE.equals(request.getSelected()) || current.isEmpty();
        if (selected) current.forEach(ps -> ps.setSelected(false));
        if (selected) productSupplierRepository.saveAll(current);

        return productSupplierRepository.save(ProductSupplier.builder()
                .product(product)
                .supplier(supplier)
                .brand(brand)
                .productUnit(unit)
                .purchasePrice(request.getPurchasePrice())
                .conversionFactorSnapshot(unit.getConversionFactor())
                .unitCodeSnapshot(unit.getCode())
                .unitNameSnapshot(unit.getName())
                .selected(selected)
                .build());
    }

    @Override
    public ProductSupplier updateSupplierOffer(Long productId, Long offerId, ProductSupplierRequest request) {
        ProductSupplier offer = productSupplierRepository.findById(offerId)
                .filter(item -> item.getProduct().getId().equals(productId))
                .orElseThrow(() -> new IllegalArgumentException("La oferta no existe para este producto."));
        Supplier supplier = supplierRepository.findById(request.getSupplierId())
                .orElseThrow(() -> new IllegalArgumentException("Proveedor no encontrado"));
        Brand brand = brandRepository.findByIdAndActiveTrue(request.getBrandId())
                .orElseThrow(() -> new IllegalArgumentException("La marca no existe o está inactiva."));
        ProductUnit unit = resolvePurchaseUnit(productId, request.getProductUnitId());
        if (!Boolean.TRUE.equals(supplier.getActivo())) {
            throw new IllegalStateException("No se pueden registrar ofertas de un proveedor inactivo.");
        }
        validatePrice(request);
        ensureUniqueOffer(productId, request, offerId);

        offer.setSupplier(supplier);
        offer.setBrand(brand);
        offer.setProductUnit(unit);
        offer.setPurchasePrice(request.getPurchasePrice());
        offer.setConversionFactorSnapshot(unit.getConversionFactor());
        offer.setUnitCodeSnapshot(unit.getCode());
        offer.setUnitNameSnapshot(unit.getName());
        return productSupplierRepository.save(offer);
    }

    private ProductUnit resolvePurchaseUnit(Long productId, Long unitId) {
        ProductUnit unit = productUnitRepository.findByIdAndProductId(unitId, productId)
                .orElseThrow(() -> new IllegalArgumentException("La presentación no pertenece al producto."));
        if (!Boolean.TRUE.equals(unit.getActive()) || !Boolean.TRUE.equals(unit.getPurchaseAllowed())) {
            throw new IllegalStateException("La presentación seleccionada no está habilitada para compras.");
        }
        if (unit.getConversionFactor() == null || unit.getConversionFactor().signum() <= 0) {
            throw new IllegalArgumentException("La presentación debe tener un factor de conversión mayor que cero.");
        }
        return unit;
    }

    private void validatePrice(ProductSupplierRequest request) {
        if (request.getPurchasePrice() == null || request.getPurchasePrice().signum() < 0) {
            throw new IllegalArgumentException("El precio de compra no puede ser negativo.");
        }
    }

    private void ensureUniqueOffer(Long productId, ProductSupplierRequest request, Long currentOfferId) {
        productSupplierRepository.findByProductIdAndSupplierIdAndBrandIdAndProductUnitId(
                productId, request.getSupplierId(), request.getBrandId(), request.getProductUnitId())
                .filter(existing -> currentOfferId == null || !existing.getId().equals(currentOfferId))
                .ifPresent(existing -> {
                    throw new IllegalStateException("Ya existe una oferta de este proveedor para la misma marca y presentación.");
                });
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProductSupplier> getSuppliersByProduct(Long productId) {
        if (!productRepository.existsById(productId)) {
            throw new IllegalArgumentException("Producto no encontrado");
        }
        return productSupplierRepository.findByProductIdOrderByPurchasePriceAsc(productId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProductSupplier> getProductsBySupplier(Long supplierId) {
        if (!supplierRepository.existsById(supplierId)) {
            throw new IllegalArgumentException("Proveedor no encontrado");
        }
        return productSupplierRepository.findBySupplierIdOrderByPurchasePriceAsc(supplierId);
    }
}