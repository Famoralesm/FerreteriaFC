package com.ferreteria.fc.domain.dto.mapper;

import com.ferreteria.fc.domain.dto.response.ProductSupplierResponse;
import com.ferreteria.fc.persistence.entity.ProductSupplier;
import org.springframework.stereotype.Component;
import java.util.List;

@Component
public class ProductSupplierMapper {
    public ProductSupplierResponse toResponse(ProductSupplier ps) {
        if (ps == null) return null;
        return ProductSupplierResponse.builder()
                .id(ps.getId())
                .productoId(ps.getProduct().getId())
                .nombreProducto(ps.getProduct().getName())
                .proveedorId(ps.getSupplier().getId())
                .nombreProveedor(ps.getSupplier().getNombre())
                .telefonoProveedor(ps.getSupplier().getTelefono())
                .emailProveedor(ps.getSupplier().getEmail())
                .precioCompra(ps.getPurchasePrice())
                .seleccionado(ps.getSelected())
                .build();
    }
    public List<ProductSupplierResponse> toListResponse(List<ProductSupplier> list) {
        return list.stream().map(this::toResponse).toList();
    }
}
