package com.ferreteria.fc.domain.dto.mapper;

import com.ferreteria.fc.domain.dto.response.ProductSupplierResponse;
import com.ferreteria.fc.persistence.entity.ProductSupplier;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

@Component
public class ProductSupplierMapper {
    public ProductSupplierResponse toResponse(ProductSupplier ps) {
        if (ps == null) return null;
        BigDecimal factor = ps.getConversionFactorSnapshot() != null
                && ps.getConversionFactorSnapshot().signum() > 0
                ? ps.getConversionFactorSnapshot() : BigDecimal.ONE;
        BigDecimal basePrice = ps.getPurchasePrice().divide(factor, 4, RoundingMode.HALF_UP);
        String baseUnitName = ps.getProduct().getBaseUnit() != null
                ? ps.getProduct().getBaseUnit().getName() : ps.getProduct().getUnit();

        return ProductSupplierResponse.builder()
                .id(ps.getId())
                .productoId(ps.getProduct().getId())
                .nombreProducto(ps.getProduct().getName())
                .proveedorId(ps.getSupplier().getId())
                .nombreProveedor(ps.getSupplier().getNombre())
                .telefonoProveedor(ps.getSupplier().getTelefono())
                .emailProveedor(ps.getSupplier().getEmail())
                .precioCompra(ps.getPurchasePrice())
                .marcaId(ps.getBrand() != null ? ps.getBrand().getId() : null)
                .nombreMarca(ps.getBrand() != null ? ps.getBrand().getName() : ps.getProduct().getBrand())
                .presentacionId(ps.getProductUnit() != null ? ps.getProductUnit().getId() : null)
                .codigoPresentacion(ps.getUnitCodeSnapshot())
                .nombrePresentacion(ps.getUnitNameSnapshot())
                .factorConversion(factor)
                .precioUnidadBase(basePrice)
                .unidadBaseProducto(baseUnitName)
                .seleccionado(ps.getSelected())
                .build();
    }

    public List<ProductSupplierResponse> toListResponse(List<ProductSupplier> list) {
        return list.stream().map(this::toResponse).toList();
    }
}