
package com.ferreteria.fc.domain.service.Impl;

import com.ferreteria.fc.domain.dto.request.InventoryMovementRequest;
import com.ferreteria.fc.domain.dto.response.InventoryMovementResponse;
import com.ferreteria.fc.domain.enums.MovementType;
import com.ferreteria.fc.domain.repository.InventoryMovementRepository;
import com.ferreteria.fc.domain.repository.ProductRepository;
import com.ferreteria.fc.domain.service.InventoryService;
import com.ferreteria.fc.persistence.entity.InventoryMovement;
import com.ferreteria.fc.persistence.entity.Product;
import com.ferreteria.fc.persistence.entity.User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class InventoryServiceImpl implements InventoryService {

    private final ProductRepository productRepository;
    private final InventoryMovementRepository movementRepository;

    public InventoryServiceImpl(
            ProductRepository productRepository,
            InventoryMovementRepository movementRepository
    ) {
        this.productRepository = productRepository;
        this.movementRepository = movementRepository;
    }

    @Override
    public InventoryMovementResponse registerEntry(
            InventoryMovementRequest request,
            User usuario
    ) {
        validarCantidadPositiva(request.getCantidad());

        return registrarMovimiento(
                request,
                usuario,
                MovementType.ENTRY,
                request.getCantidad()
        );
    }

    @Override
    public InventoryMovementResponse registerExit(
            InventoryMovementRequest request,
            User usuario
    ) {
        validarCantidadPositiva(request.getCantidad());

        return registrarMovimiento(
                request,
                usuario,
                MovementType.EXIT,
                -request.getCantidad()
        );
    }

    @Override
    public InventoryMovementResponse registerAdjustment(
            InventoryMovementRequest request,
            User usuario
    ) {
        if (request.getCantidad() == null
                || request.getCantidad() == 0
                || request.getCantidad() == Integer.MIN_VALUE) {
            throw new IllegalArgumentException(
                    "La diferencia del ajuste debe ser distinta de cero"
            );
        }

        return registrarMovimiento(
                request,
                usuario,
                MovementType.ADJUSTMENT,
                request.getCantidad()
        );
    }

    private InventoryMovementResponse registrarMovimiento(
            InventoryMovementRequest request,
            User usuario,
            MovementType tipo,
            Integer diferencia
    ) {
        if (request.getProductoId() == null) {
            throw new IllegalArgumentException(
                    "Debe indicar el producto"
            );
        }



        Product producto = productRepository
                .findByIdForUpdate(request.getProductoId())
                .orElseThrow(() -> new IllegalArgumentException(
                        "Producto no encontrado: " + request.getProductoId()
                ));

        validarReferenciaDuplicada(request, tipo);

        if (!Boolean.TRUE.equals(producto.getActive())) {
            throw new IllegalArgumentException(
                    "No se pueden registrar movimientos para un producto inactivo"
            );
        }

        Integer stockAnterior = producto.getStock() != null
                ? producto.getStock()
                : 0;

        long resultado = (long) stockAnterior + diferencia;

        if (resultado < 0) {
            throw new IllegalArgumentException(
                    "Stock insuficiente. Disponible: " + stockAnterior
            );
        }

        if (resultado > Integer.MAX_VALUE) {
            throw new IllegalArgumentException(
                    "La cantidad supera el límite de stock permitido"
            );
        }

        Integer stockNuevo = (int) resultado;

        producto.setStock(stockNuevo);
        productRepository.save(producto);

        InventoryMovement movimiento = InventoryMovement.builder()
                .producto(producto)
                .tipoMovimiento(tipo)
                .cantidad(Math.abs(diferencia))
                .stockAnterior(stockAnterior)
                .stockNuevo(stockNuevo)
                .motivo(request.getMotivo())
                .tipoReferencia(request.getTipoReferencia())
                .referenciaId(request.getReferenciaId())
                .usuario(usuario)
                .build();

        InventoryMovement guardado = movementRepository.save(movimiento);

        return toResponse(guardado);
    }

    private void validarCantidadPositiva(Integer cantidad) {
        if (cantidad == null || cantidad <= 0) {
            throw new IllegalArgumentException(
                    "La cantidad debe ser mayor que cero"
            );
        }
    }

    private void validarReferenciaDuplicada(
            InventoryMovementRequest request,
            MovementType tipo
    ) {
        if (request.getTipoReferencia() == null
                || request.getTipoReferencia().isBlank()
                || request.getReferenciaId() == null) {
            return;
        }

        boolean existe = movementRepository
                .existsByProductoIdAndTipoMovimientoAndTipoReferenciaAndReferenciaId(
                        request.getProductoId(),
                        tipo,
                        request.getTipoReferencia(),
                        request.getReferenciaId()
                );

        if (existe) {
            throw new IllegalArgumentException(
                    "Este movimiento ya fue registrado anteriormente"
            );
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<InventoryMovementResponse> getMovements(Long productoId) {

        if (!productRepository.existsById(productoId)) {
            throw new IllegalArgumentException(
                    "Producto no encontrado: " + productoId
            );
        }

        return movementRepository
                .findByProductoIdOrderByFechaCreacionDesc(productoId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    private InventoryMovementResponse toResponse(
            InventoryMovement movimiento
    ) {
        Product producto = movimiento.getProducto();

        return InventoryMovementResponse.builder()
                .id(movimiento.getId())
                .productoId(producto.getId())
                .sku(producto.getSku())
                .nombreProducto(producto.getName())
                .tipoMovimiento(movimiento.getTipoMovimiento())
                .cantidad(movimiento.getCantidad())
                .stockAnterior(movimiento.getStockAnterior())
                .stockNuevo(movimiento.getStockNuevo())
                .motivo(movimiento.getMotivo())
                .tipoReferencia(movimiento.getTipoReferencia())
                .referenciaId(movimiento.getReferenciaId())
                .usuario(movimiento.getUsuario() != null
                        ? movimiento.getUsuario().getName()
                        : null)
                .fechaCreacion(movimiento.getFechaCreacion())
                .build();
    }
}
