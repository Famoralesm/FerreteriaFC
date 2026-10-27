package com.ferreteria.fc.domain.service.Impl;

import com.ferreteria.fc.domain.repository.ProductSupplierRepository;
import com.ferreteria.fc.domain.service.SupplierService;
import com.ferreteria.fc.persistence.entity.ProductSupplier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class SupplierServiceImpl implements SupplierService {

    private final ProductSupplierRepository productSupplierRepository;

    public SupplierServiceImpl(ProductSupplierRepository productSupplierRepository) {
        this.productSupplierRepository = productSupplierRepository;
    }

    @Transactional
    public void selectSupplier(
            Long productId,
            Long productSupplierId) {

        List<ProductSupplier> suppliers =
                productSupplierRepository.findByProductId(productId);

        ProductSupplier selected =
                suppliers.stream()
                        .filter(ps -> ps.getId().equals(productSupplierId))
                        .findFirst()
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "Proveedor no asociado al producto"
                                ));

        suppliers.forEach(ps -> ps.setSelected(false));

        selected.setSelected(true);

        productSupplierRepository.saveAll(suppliers);
    }
}
