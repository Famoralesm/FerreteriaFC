package com.ferreteria.fc.domain.service.Impl;

import com.ferreteria.fc.domain.dto.request.SupplierRequest;
import com.ferreteria.fc.domain.repository.ProductSupplierRepository;
import com.ferreteria.fc.domain.repository.SupplierRepository;
import com.ferreteria.fc.domain.service.SupplierService;
import com.ferreteria.fc.persistence.entity.ProductSupplier;
import com.ferreteria.fc.persistence.entity.Supplier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Service
@Transactional
public class SupplierServiceImpl implements SupplierService {
    private final ProductSupplierRepository productSupplierRepository;
    private final SupplierRepository supplierRepository;

    public SupplierServiceImpl(ProductSupplierRepository productSupplierRepository, SupplierRepository supplierRepository) {
        this.productSupplierRepository = productSupplierRepository;
        this.supplierRepository = supplierRepository;
    }

    @Override
    public void selectSupplier(Long productId, Long productSupplierId) {
        List<ProductSupplier> suppliers = productSupplierRepository.findByProductId(productId);
        ProductSupplier selected = suppliers.stream()
                .filter(ps -> ps.getId().equals(productSupplierId))
                .findFirst().orElseThrow(() -> new IllegalArgumentException("Proveedor no asociado al producto"));
        suppliers.forEach(ps -> ps.setSelected(ps.getId().equals(productSupplierId)));
        productSupplierRepository.saveAll(suppliers);
    }

    @Override
    public Supplier create(SupplierRequest request) {
        return supplierRepository.save(Supplier.builder().nombre(request.getNombre().trim())
                .telefono(request.getTelefono()).email(request.getEmail()).build());
    }

    @Override
    public Supplier update(Long id, SupplierRequest request) {
        Supplier supplier = get(id);
        supplier.setNombre(request.getNombre().trim());
        supplier.setTelefono(request.getTelefono());
        supplier.setEmail(request.getEmail());
        return supplierRepository.save(supplier);
    }

    @Override
    @Transactional(readOnly = true)
    public Supplier get(Long id) {
        return supplierRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Proveedor no encontrado: " + id));
    }

    @Override
    @Transactional(readOnly = true)
    public List<Supplier> getAll() {
        return supplierRepository.findAllByOrderByNombreAsc();
    }
}
