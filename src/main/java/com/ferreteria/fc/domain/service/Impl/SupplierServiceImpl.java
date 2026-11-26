package com.ferreteria.fc.domain.service.Impl;

import com.ferreteria.fc.domain.dto.request.SupplierRequest;
import com.ferreteria.fc.domain.repository.ProductSupplierRepository;
import com.ferreteria.fc.domain.repository.SupplierRepository;
import com.ferreteria.fc.domain.repository.CiudadRepository;
import com.ferreteria.fc.domain.repository.ComunaRepository;
import com.ferreteria.fc.persistence.entity.Ciudad;
import com.ferreteria.fc.persistence.entity.Comuna;
import com.ferreteria.fc.domain.service.SupplierService;
import com.ferreteria.fc.domain.service.AuditLogService;
import com.ferreteria.fc.persistence.entity.ProductSupplier;
import com.ferreteria.fc.persistence.entity.Supplier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.Objects;

@Service
@Transactional
public class SupplierServiceImpl implements SupplierService {
    private final ProductSupplierRepository productSupplierRepository;
    private final SupplierRepository supplierRepository;
    private final CiudadRepository ciudadRepository;
    private final ComunaRepository comunaRepository;
    private final AuditLogService auditLogService;

    public SupplierServiceImpl(ProductSupplierRepository productSupplierRepository, SupplierRepository supplierRepository, CiudadRepository ciudadRepository, ComunaRepository comunaRepository, AuditLogService auditLogService) {
        this.productSupplierRepository = productSupplierRepository;
        this.supplierRepository = supplierRepository;
        this.ciudadRepository = ciudadRepository;
        this.comunaRepository = comunaRepository;
        this.auditLogService = auditLogService;
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
        validarCiudadComuna(request.getCiudad(), request.getComuna());
        Supplier saved = supplierRepository.save(Supplier.builder().rut(rut).nombre(request.getNombre().trim())
                .razonSocial(request.getRazonSocial()).giro(request.getGiro())
                .direccion(request.getDireccion()).comuna(resolverComuna(request.getComuna()))
                .ciudad(resolverCiudad(request.getCiudad())).telefono(request.getTelefono())
                .email(request.getEmail()).activo(request.getActivo() == null ? true : request.getActivo()).build());
        auditLogService.recordForCurrentUser(
                "SUPPLIERS", "SUPPLIER", saved.getId().toString(), "CREATED", null,
                null, Map.of("active", saved.isActivo(), "capturedFields",
                        List.of("rut", "nombre", "razonSocial", "giro", "direccion", "ciudad", "comuna", "telefono", "email")),
                "Proveedor creado (datos de contacto omitidos de la bitácora)", "WEB"
        );
        return saved;
    }

    @Override
    public Supplier update(Long id, SupplierRequest request) {
        Supplier supplier = get(id);
        String rut = normalizarRut(request.getRut());
        if (rut != null && supplierRepository.findByRut(rut).filter(s -> !s.getId().equals(id)).isPresent())
            throw new IllegalStateException("El RUT ya está registrado por otro proveedor");
        validarCiudadComuna(request.getCiudad(), request.getComuna());
        String rutAnterior = supplier.getRut();
        String nombreAnterior = supplier.getNombre();
        String razonSocialAnterior = supplier.getRazonSocial();
        String giroAnterior = supplier.getGiro();
        String direccionAnterior = supplier.getDireccion();
        String telefonoAnterior = supplier.getTelefono();
        String emailAnterior = supplier.getEmail();
        Long ciudadAnterior = supplier.getCiudad() != null ? supplier.getCiudad().getId() : null;
        Long comunaAnterior = supplier.getComuna() != null ? supplier.getComuna().getId() : null;
        boolean activoAnterior = supplier.isActivo();

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
        Supplier updated = supplierRepository.save(supplier);
        List<String> changedFields = new ArrayList<>();
        if (!Objects.equals(rutAnterior, updated.getRut())) changedFields.add("rut");
        if (!Objects.equals(nombreAnterior, updated.getNombre())) changedFields.add("nombre");
        if (!Objects.equals(razonSocialAnterior, updated.getRazonSocial())) changedFields.add("razonSocial");
        if (!Objects.equals(giroAnterior, updated.getGiro())) changedFields.add("giro");
        if (!Objects.equals(direccionAnterior, updated.getDireccion())) changedFields.add("direccion");
        if (!Objects.equals(telefonoAnterior, updated.getTelefono())) changedFields.add("telefono");
        if (!Objects.equals(emailAnterior, updated.getEmail())) changedFields.add("email");
        if (!Objects.equals(ciudadAnterior, updated.getCiudad() != null ? updated.getCiudad().getId() : null)) changedFields.add("ciudad");
        if (!Objects.equals(comunaAnterior, updated.getComuna() != null ? updated.getComuna().getId() : null)) changedFields.add("comuna");
        if (activoAnterior != updated.isActivo()) changedFields.add("activo");
        if (!changedFields.isEmpty()) {
            auditLogService.recordForCurrentUser(
                    "SUPPLIERS", "SUPPLIER", updated.getId().toString(),
                    activoAnterior && !updated.isActivo() ? "DEACTIVATED" : "UPDATED",
                    null,
                    Map.of("changedFields", changedFields),
                    Map.of("changedFields", changedFields, "active", updated.isActivo()),
                    "Proveedor actualizado; se registran los campos modificados, no sus datos de contacto",
                    "WEB"
            );
        }
        return updated;
    }

    @Override
    @Transactional(readOnly = true)
    public Supplier get(Long id) {
        return supplierRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Proveedor no encontrado: " + id));
    }

    @Override
    public void deactivate(Long id) {
        Supplier supplier = get(id);
        boolean wasActive = supplier.isActivo();
        supplier.setActivo(false);
        supplierRepository.save(supplier);
        if (wasActive) {
            auditLogService.recordForCurrentUser(
                    "SUPPLIERS", "SUPPLIER", supplier.getId().toString(), "DEACTIVATED", null,
                    Map.of("active", true), Map.of("active", false),
                    "Proveedor desactivado", "WEB"
            );
        }
    }

    private void validarCiudadComuna(Ciudad ciudad, Comuna comuna) {
        if (ciudad == null || ciudad.getId() == null || comuna == null || comuna.getId() == null) {
            throw new IllegalArgumentException("Debe seleccionar ciudad y comuna");
        }
        Comuna comunaRegistrada = comunaRepository.findById(comuna.getId())
                .orElseThrow(() -> new IllegalArgumentException("Comuna no encontrada"));
        if (!comunaRegistrada.getCiudad().getId().equals(ciudad.getId())) {
            throw new IllegalArgumentException("La comuna seleccionada no pertenece a la ciudad indicada");
        }
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
