package com.ferreteria.fc.domain.service.Impl;

import com.ferreteria.fc.domain.dto.request.ProductSupplierRequest;
import com.ferreteria.fc.domain.repository.ProductRepository;
import com.ferreteria.fc.domain.repository.ProductSupplierRepository;
import com.ferreteria.fc.domain.repository.SupplierRepository;
import com.ferreteria.fc.domain.service.ProductSupplierService;
import com.ferreteria.fc.persistence.entity.Product;
import com.ferreteria.fc.persistence.entity.ProductSupplier;
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

    public ProductSupplierServiceImpl(ProductRepository productRepository,
            SupplierRepository supplierRepository, ProductSupplierRepository productSupplierRepository) {
        this.productRepository = productRepository;
        this.supplierRepository = supplierRepository;
        this.productSupplierRepository = productSupplierRepository;
    }

    @Override
    public ProductSupplier addSupplierToProduct(Long productId, ProductSupplierRequest request) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new IllegalArgumentException("Producto no encontrado"));
        Supplier supplier = supplierRepository.findById(request.getSupplierId())
                .orElseThrow(() -> new IllegalArgumentException("Proveedor no encontrado"));

        if (productSupplierRepository.findByProductIdAndSupplierId(productId, request.getSupplierId()).isPresent())
            throw new IllegalStateException("El proveedor ya está asociado al producto");

        List<ProductSupplier> current = productSupplierRepository.findByProductId(productId);
        boolean selected = Boolean.TRUE.equals(request.getSelected()) || current.isEmpty();
        if (selected) current.forEach(ps -> ps.setSelected(false));
        if (selected) productSupplierRepository.saveAll(current);

        return productSupplierRepository.save(ProductSupplier.builder()
                .product(product).supplier(supplier).purchasePrice(request.getPurchasePrice())
                .selected(selected).build());
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProductSupplier> getSuppliersByProduct(Long productId) {
        if (!productRepository.existsById(productId)) throw new IllegalArgumentException("Producto no encontrado");
        return productSupplierRepository.findByProductIdOrderByPurchasePriceAsc(productId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProductSupplier> getProductsBySupplier(Long supplierId) {
        if (!supplierRepository.existsById(supplierId)) throw new IllegalArgumentException("Proveedor no encontrado");
        return productSupplierRepository.findBySupplierIdOrderByPurchasePriceAsc(supplierId);
    }
}
