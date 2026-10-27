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

    public ProductSupplierServiceImpl(ProductRepository productRepository, SupplierRepository supplierRepository, ProductSupplierRepository productSupplierRepository) {
        this.productRepository = productRepository;
        this.supplierRepository = supplierRepository;
        this.productSupplierRepository = productSupplierRepository;
    }


    @Transactional
    public ProductSupplier addSupplierToProduct(
            Long productId,
            ProductSupplierRequest request) {

        Product product = productRepository.findById(productId)
                .orElseThrow(() ->
                        new RuntimeException("Producto no encontrado"));

        Supplier supplier = supplierRepository.findById(request.getSupplierId())
                .orElseThrow(() ->
                        new RuntimeException("Proveedor no encontrado"));

        productSupplierRepository
                .findByProductIdAndSupplierId(
                        productId,
                        request.getSupplierId()
                )
                .ifPresent(existing -> {
                    throw new RuntimeException(
                            "El proveedor ya está asociado al producto"
                    );
                });

        if (Boolean.TRUE.equals(request.getSelected())) {

            List<ProductSupplier> suppliers =
                    productSupplierRepository.findByProductId(productId);

            suppliers.forEach(ps -> ps.setSelected(false));

            productSupplierRepository.saveAll(suppliers);
        }

        ProductSupplier productSupplier =
                ProductSupplier.builder()
                        .product(product)
                        .supplier(supplier)
                        .purchasePrice(request.getPurchasePrice())
                        .selected(
                                Boolean.TRUE.equals(request.getSelected())
                        )
                        .build();

        return productSupplierRepository.save(productSupplier);
    }

}
