
package com.ferreteria.fc.domain.service.Impl;

import com.ferreteria.fc.domain.dto.request.InventoryMovementRequest;
import com.ferreteria.fc.domain.dto.response.InventoryMovementResponse;
import com.ferreteria.fc.domain.enums.MovementType;
import com.ferreteria.fc.domain.enums.Role;
import com.ferreteria.fc.domain.repository.InventoryMovementRepository;
import com.ferreteria.fc.domain.repository.InventoryCountRepository;
import com.ferreteria.fc.domain.enums.InventoryCountStatus;
import com.ferreteria.fc.domain.repository.ProductRepository;
import com.ferreteria.fc.domain.repository.ProductStockRepository;
import com.ferreteria.fc.domain.repository.StoreRepository;
import com.ferreteria.fc.domain.service.InventoryService;
import com.ferreteria.fc.persistence.entity.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.math.BigDecimal;

@Service
@Transactional
public class InventoryServiceImpl implements InventoryService {

    private final ProductRepository productRepository;
    private final ProductStockRepository productStockRepository;
    private final StoreRepository storeRepository;
    private final InventoryMovementRepository movementRepository;
    private final InventoryCountRepository inventoryCountRepository;

    public InventoryServiceImpl(
            ProductRepository productRepository,
            ProductStockRepository productStockRepository,
            StoreRepository storeRepository,
            InventoryMovementRepository movementRepository,
            InventoryCountRepository inventoryCountRepository
    ) {
        this.productRepository = productRepository;
        this.productStockRepository = productStockRepository;
        this.storeRepository = storeRepository;
        this.movementRepository = movementRepository;
        this.inventoryCountRepository = inventoryCountRepository;
    }

    @Override
    public InventoryMovementResponse registerEntry(
            InventoryMovementRequest request,
            User usuario
    ) {
        validarCantidadPositiva(request.getCantidad());

        return registrarMovimiento(
                request, usuario, MovementType.ENTRY, request.getCantidad()
        );
    }

    @Override
    public InventoryMovementResponse registerExit(
            InventoryMovementRequest request,
            User usuario
    ) {
        validarCantidadPositiva(request.getCantidad());

        return registrarMovimiento(
                request, usuario, MovementType.EXIT, request.getCantidad().negate()
        );
    }

    @Override
    public InventoryMovementResponse registerReturn(
            InventoryMovementRequest request,
            User usuario
    ) {
        validarCantidadPositiva(request.getCantidad());
        return registrarMovimiento(
                request, usuario, MovementType.RETURN, request.getCantidad()
        );
    }

    @Override
    public InventoryMovementResponse registerAdjustment(
            InventoryMovementRequest request,
            User usuario
    ) {
        if (request.getCantidad() == null
                || request.getCantidad().compareTo(BigDecimal.ZERO) == 0) {
            throw new IllegalArgumentException(
                    "La diferencia del ajuste debe ser distinta de cero"
            );
        }

        return registrarMovimiento(
                request, usuario, MovementType.ADJUSTMENT,
                request.getCantidad()
        );
    }

    private InventoryMovementResponse registrarMovimiento(
            InventoryMovementRequest request,
            User usuario,
            MovementType tipo,
            BigDecimal diferencia
    ) {
        if (request.getProductoId() == null) {
            throw new IllegalArgumentException("Debe indicar el producto");
        }

        Long sucursalId = resolverSucursalId(request, usuario);
        if (inventoryCountRepository.existsBySucursalIdAndStatus(sucursalId, InventoryCountStatus.IN_PROGRESS)) {
            throw new IllegalStateException("No se permiten movimientos mientras exista un inventario físico en curso");
        }

        Store sucursal = storeRepository.findById(sucursalId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Sucursal no encontrada: " + sucursalId
                ));

        Product producto = productRepository
                .findByIdForUpdate(request.getProductoId())
                .orElseThrow(() -> new IllegalArgumentException(
                        "Producto no encontrado: " + request.getProductoId()
                ));

        if (!Boolean.TRUE.equals(producto.getActive())) {
            throw new IllegalArgumentException(
                    "No se pueden registrar movimientos para un producto inactivo"
            );
        }

        ProductStock productoStock = productStockRepository
                .findByProductoAndSucursalForUpdate(
                        producto.getId(), sucursalId
                )
                .orElseThrow(() -> new IllegalArgumentException(
                        "El producto no tiene inventario inicializado en "
                                + sucursal.getNombre()
                ));

        validarReferenciaDuplicada(request, tipo, sucursalId);

        BigDecimal stockAnterior = productoStock.getStock() != null
                ? productoStock.getStock()
                : BigDecimal.ZERO;

        BigDecimal resultado = stockAnterior.add(diferencia);

        if (resultado.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException(
                    "Stock insuficiente en " + sucursal.getNombre()
                            + ". Disponible: " + stockAnterior.toPlainString()
            );
        }

        if (resultado.compareTo(new BigDecimal("9999999999.9999")) > 0) {
            throw new IllegalArgumentException(
                    "La cantidad supera el límite de stock permitido"
            );
        }

        BigDecimal stockNuevo = resultado;

        productoStock.setStock(stockNuevo);
        productStockRepository.save(productoStock);

        InventoryMovement movimiento = InventoryMovement.builder()
                .producto(producto)
                .sucursal(sucursal)
                .tipoMovimiento(tipo)
                .cantidad(diferencia.abs())
                .stockAnterior(stockAnterior)
                .stockNuevo(stockNuevo)
                .motivo(request.getMotivo())
                .tipoReferencia(request.getTipoReferencia())
                .referenciaId(request.getReferenciaId())
                .usuario(usuario)
                .build();

        InventoryMovement savedMovement = movementRepository.save(movimiento);
        return toResponse(savedMovement);
    }

    private Long resolverSucursalId(
            InventoryMovementRequest request,
            User usuario
    ) {
        if (usuario == null) {
            throw new IllegalArgumentException(
                    "No se pudo identificar al usuario autenticado"
            );
        }

        // El administrador puede seleccionar la sucursal.
        if (usuario.getRole() == Role.ADMIN) {
            if (request.getSucursalId() == null) {
                throw new IllegalArgumentException(
                        "El administrador debe indicar la sucursal"
                );
            }

            return request.getSucursalId();
        }

        // Los demás roles siempre operan en su sucursal asignada.
        if (usuario.getSucursal() == null) {
            throw new IllegalArgumentException(
                    "El usuario no tiene una sucursal asignada"
            );
        }

        return usuario.getSucursal().getId();
    }

    private void validarCantidadPositiva(BigDecimal cantidad) {
        if (cantidad == null || cantidad.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException(
                    "La cantidad debe ser mayor que cero"
            );
        }
    }

    private void validarReferenciaDuplicada(
            InventoryMovementRequest request,
            MovementType tipo,
            Long sucursalId
    ) {
        if (request.getTipoReferencia() == null
                || request.getTipoReferencia().isBlank()
                || request.getReferenciaId() == null) {
            return;
        }

        boolean existe = movementRepository
                .existsByProductoIdAndSucursalIdAndTipoMovimientoAndTipoReferenciaAndReferenciaId(
                        request.getProductoId(),
                        sucursalId,
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
    public List<InventoryMovementResponse> getMovements(
            Long productoId,
            Long sucursalId,
            User usuario
    ) {
        if (!productRepository.existsById(productoId)) {
            throw new IllegalArgumentException(
                    "Producto no encontrado: " + productoId
            );
        }

        Long sucursalAutorizada = resolverSucursalId(
                InventoryMovementRequest.builder()
                        .sucursalId(sucursalId)
                        .build(),
                usuario
        );

        return movementRepository
                .findByProductoIdAndSucursalIdOrderByFechaCreacionDesc(
                        productoId, sucursalAutorizada
                )
                .stream()
                .map(this::toResponse)
                .toList();
    }

    private InventoryMovementResponse toResponse(
            InventoryMovement movimiento
    ) {
        Product producto = movimiento.getProducto();
        Store sucursal = movimiento.getSucursal();

        return InventoryMovementResponse.builder()
                .id(movimiento.getId())
                .productoId(producto.getId())
                .sku(producto.getSku())
                .nombreProducto(producto.getName())
                .sucursalId(sucursal.getId())
                .nombreSucursal(sucursal.getNombre())
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
