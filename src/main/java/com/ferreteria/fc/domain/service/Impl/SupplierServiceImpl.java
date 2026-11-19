package com.ferreteria.fc.domain.service.Impl;

import com.ferreteria.fc.domain.dto.request.SupplierRequest;
import com.ferreteria.fc.domain.repository.ProductSupplierRepository;
import com.ferreteria.fc.domain.repository.SupplierRepository;
import com.ferreteria.fc.domain.repository.CiudadRepository;
import com.ferreteria.fc.domain.repository.ComunaRepository;
import com.ferreteria.fc.persistence.entity.Ciudad;
import com.ferreteria.fc.persistence.entity.Comuna;
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
    private final CiudadRepository ciudadRepository;
    private final ComunaRepository comunaRepository;

    public SupplierServiceImpl(ProductSupplierRepository productSupplierRepository, SupplierRepository supplierRepository, CiudadRepository ciudadRepository, ComunaRepository comunaRepository) {
        this.productSupplierRepository = productSupplierRepository;
        this.supplierRepository = supplierRepository;
        this.ciudadRepository = ciudadRepository;
        this.comunaRepository = comunaRepository;
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
        String rut = normalizarRut(request.getRut());
        if (rut != null && supplierRepository.existsByRut(rut))
            throw new IllegalStateException("Ya existe un proveedor con RUT " + rut);
        return supplierRepository.save(Supplier.builder().rut(rut).nombre(request.getNombre().trim())
                .razonSocial(request.getRazonSocial()).giro(request.getGiro())
                .direccion(request.getDireccion()).comuna(resolverComuna(request.getComuna()))
                .ciudad(resolverCiudad(request.getCiudad())).telefono(request.getTelefono())
                .email(request.getEmail()).activo(request.getActivo() == null ? true : request.getActivo()).build());
    }

    @Override
    public Supplier update(Long id, SupplierRequest request) {
        Supplier supplier = get(id);
        String rut = normalizarRut(request.getRut());
        if (rut != null && supplierRepository.findByRut(rut).filter(s -> !s.getId().equals(id)).isPresent())
            throw new IllegalStateException("El RUT ya está registrado por otro proveedor");
        supplier.setRut(rut);
        supplier.setNombre(request.getNombre().trim());
        supplier.setRazonSocial(request.getRazonSocial());
        supplier.setGiro(request.getGiro());
        supplier.setDireccion(request.getDireccion());
        supplier.setComuna(resolverComuna(request.getComuna()));
        supplier.setCiudad(resolverCiudad(request.getCiudad()));
        supplier.setTelefono(request.getTelefono());
        supplier.setEmail(request.getEmail());
        if (request.getActivo() != null) supplier.setActivo(request.getActivo());
        return supplierRepository.save(supplier);
    }

    @Override
    @Transactional(readOnly = true)
    public Supplier get(Long id) {
        return supplierRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Proveedor no encontrado: " + id));
    }

    private String normalizarRut(String rut) {
        if (rut == null || rut.isBlank()) return null;
        return rut.trim().replace(".", "").replace("-", "").toUpperCase();
    }

    private Ciudad resolverCiudad(Ciudad ciudad) {
        if (ciudad == null || ciudad.getId() == null) return null;
        return ciudadRepository.findById(ciudad.getId())
                .orElseThrow(() -> new IllegalArgumentException("Ciudad no encontrada"));
    }

    private Comuna resolverComuna(Comuna comuna) {
        if (comuna == null || comuna.getId() == null) return null;
        return comunaRepository.findById(comuna.getId())
                .orElseThrow(() -> new IllegalArgumentException("Comuna no encontrada"));
    }

    @Override
    @Transactional(readOnly = true)
    public List<Supplier> getAll() {
        return supplierRepository.findAllByOrderByNombreAsc();
    }
}
