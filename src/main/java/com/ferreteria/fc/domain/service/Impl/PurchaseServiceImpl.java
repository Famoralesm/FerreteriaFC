package com.ferreteria.fc.domain.service.Impl;

import com.ferreteria.fc.domain.enums.InventoryCountStatus;
import com.ferreteria.fc.domain.enums.MovementType;
import com.ferreteria.fc.domain.enums.PurchaseStatus;
import com.ferreteria.fc.domain.enums.Role;
import com.ferreteria.fc.domain.repository.*;
import com.ferreteria.fc.domain.service.PurchaseService;
import com.ferreteria.fc.dto.purchase.PurchaseItemRequest;
import com.ferreteria.fc.dto.purchase.PurchaseItemResponse;
import com.ferreteria.fc.dto.purchase.PurchaseRequest;
import com.ferreteria.fc.dto.purchase.PurchaseResponse;
import com.ferreteria.fc.persistence.entity.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

@Service
@Transactional
public class PurchaseServiceImpl implements PurchaseService {

    private static final BigDecimal FACTOR_UNIDAD_BASE = BigDecimal.ONE;
    private static final BigDecimal MAX_CANTIDAD = new BigDecimal("9999999999.9999");
    private static final String TIPO_REFERENCIA_COMPRA = "PURCHASE";

    private final PurchaseRepository purchaseRepository;
    private final SupplierRepository supplierRepository;
    private final StoreRepository storeRepository;
    private final ProductRepository productRepository;
    private final ProductUnitRepository productUnitRepository;
    private final ProductStockRepository productStockRepository;
    private final InventoryMovementRepository inventoryMovementRepository;
    private final InventoryCountRepository inventoryCountRepository;

    public PurchaseServiceImpl(
            PurchaseRepository purchaseRepository,
            SupplierRepository supplierRepository,
            StoreRepository storeRepository,
            ProductRepository productRepository,
            ProductUnitRepository productUnitRepository,
            ProductStockRepository productStockRepository,
            InventoryMovementRepository inventoryMovementRepository,
            InventoryCountRepository inventoryCountRepository
    ) {
        this.purchaseRepository = purchaseRepository;
        this.supplierRepository = supplierRepository;
        this.storeRepository = storeRepository;
        this.productRepository = productRepository;
        this.productUnitRepository = productUnitRepository;
        this.productStockRepository = productStockRepository;
        this.inventoryMovementRepository = inventoryMovementRepository;
        this.inventoryCountRepository = inventoryCountRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public List<PurchaseResponse> getAll(User usuario) {
        validarUsuario(usuario);
        List<Purchase> purchases = usuario.getRole() == Role.ADMIN
                ? purchaseRepository.findAllByOrderByCreatedAtDesc()
                : purchaseRepository.findBySucursalIdOrderByCreatedAtDesc(
                        usuario.getSucursal() != null ? usuario.getSucursal().getId() : -1L
                );
        return purchases.stream().map(this::toResponse).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public PurchaseResponse getById(Long purchaseId, User usuario) {
        validarUsuario(usuario);
        if (purchaseId == null || purchaseId <= 0) {
            throw new IllegalArgumentException("Debe indicar una compra válida");
        }
        Purchase purchase = purchaseRepository.findWithDetailsById(purchaseId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Compra no encontrada: " + purchaseId
                ));
        validarSucursalAutorizada(purchase.getSucursal().getId(), usuario);
        return toResponse(purchase);
    }

    @Override
    public PurchaseResponse register(PurchaseRequest request, User usuario) {
        validarUsuario(usuario);

        Supplier supplier = supplierRepository.findById(request.supplierId())
                .orElseThrow(() -> new IllegalArgumentException(
                        "Proveedor no encontrado: " + request.supplierId()
                ));

        if (!Boolean.TRUE.equals(supplier.getActivo())) {
            throw new IllegalArgumentException(
                    "No se pueden registrar compras para un proveedor inactivo"
            );
        }

        Long sucursalId = resolverSucursalId(request, usuario);
        Store sucursal = storeRepository.findById(sucursalId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Sucursal no encontrada: " + sucursalId
                ));

        if (!Boolean.TRUE.equals(sucursal.getActiva())) {
            throw new IllegalArgumentException(
                    "No se pueden registrar compras para una sucursal inactiva"
            );
        }

        if (request.items() == null || request.items().isEmpty()) {
            throw new IllegalArgumentException(
                    "La compra debe incluir al menos un producto"
            );
        }

        Purchase purchase = Purchase.builder()
                .supplier(supplier)
                .sucursal(sucursal)
                .documentType(normalizarTexto(request.documentType()))
                .documentNumber(normalizarTexto(request.documentNumber()))
                .purchaseDate(request.purchaseDate())
                .status(PurchaseStatus.PENDING_RECEIPT)
                .notes(normalizarTexto(request.notes()))
                .createdBy(usuario)
                .total(BigDecimal.ZERO)
                .items(new ArrayList<>())
                .build();

        BigDecimal total = BigDecimal.ZERO;

        for (PurchaseItemRequest itemRequest : request.items()) {
            PurchaseItem item = construirDetalle(purchase, itemRequest);
            purchase.getItems().add(item);
            total = total.add(item.getSubtotal());
        }

        if (total.precision() > 15 || total.scale() > 2) {
            throw new IllegalArgumentException(
                    "El total de la compra supera el límite permitido"
            );
        }

        purchase.setTotal(total.setScale(2, RoundingMode.HALF_UP));

        // Registrar la compra no modifica ProductStock ni crea InventoryMovement.
        Purchase saved = purchaseRepository.save(purchase);
        return toResponse(saved);
    }

    @Override
    public PurchaseResponse receive(Long purchaseId, User usuario) {
        validarUsuario(usuario);

        if (purchaseId == null || purchaseId <= 0) {
            throw new IllegalArgumentException("Debe indicar una compra válida");
        }

        // El bloqueo evita que dos solicitudes reciban la misma compra simultáneamente.
        Purchase purchase = purchaseRepository.findByIdForUpdate(purchaseId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Compra no encontrada: " + purchaseId
                ));

        if (purchase.getStatus() != PurchaseStatus.PENDING_RECEIPT) {
            throw new IllegalStateException(
                    "Solo se pueden recibir compras pendientes. Estado actual: "
                            + purchase.getStatus()
            );
        }

        Long sucursalId = purchase.getSucursal().getId();
        validarSucursalAutorizada(sucursalId, usuario);

        if (inventoryCountRepository.existsBySucursalIdAndStatus(
                sucursalId, InventoryCountStatus.IN_PROGRESS)) {
            throw new IllegalStateException(
                    "No se puede recibir la compra mientras exista un inventario físico en curso"
            );
        }

        if (purchase.getItems() == null || purchase.getItems().isEmpty()) {
            throw new IllegalStateException(
                    "La compra no contiene productos para recibir"
            );
        }

        // Se consolidan líneas repetidas del mismo producto para crear un movimiento único.
        // TreeMap mantiene un orden estable de bloqueo y reduce el riesgo de deadlocks.
        Map<Long, BigDecimal> cantidadesPorProducto = new TreeMap<>();

        for (PurchaseItem item : purchase.getItems()) {
            BigDecimal cantidad = item.getQuantityBaseUnit();

            if (cantidad == null
                    || cantidad.compareTo(BigDecimal.ZERO) <= 0
                    || cantidad.compareTo(MAX_CANTIDAD) > 0) {
                throw new IllegalStateException(
                        "La cantidad convertida no es válida para el producto "
                                + item.getProductSkuSnapshot()
                );
            }

            cantidadesPorProducto.merge(
                    item.getProduct().getId(),
                    cantidad,
                    BigDecimal::add
            );
        }

        for (Map.Entry<Long, BigDecimal> entry : cantidadesPorProducto.entrySet()) {
            Long productId = entry.getKey();
            BigDecimal cantidadEntrada = entry.getValue()
                    .setScale(4, RoundingMode.UNNECESSARY);

            if (cantidadEntrada.compareTo(MAX_CANTIDAD) > 0) {
                throw new IllegalArgumentException(
                        "La cantidad total supera el límite permitido para el producto "
                                + productId
                );
            }

            Product product = productRepository.findByIdForUpdate(productId)
                    .orElseThrow(() -> new IllegalArgumentException(
                            "Producto no encontrado: " + productId
                    ));

            if (!Boolean.TRUE.equals(product.getActive())) {
                throw new IllegalArgumentException(
                        "No se puede recibir un producto inactivo: " + product.getSku()
                );
            }

            ProductStock productStock = productStockRepository
                    .findByProductoAndSucursalForUpdate(productId, sucursalId)
                    .orElseThrow(() -> new IllegalArgumentException(
                            "El producto " + product.getSku()
                                    + " no tiene inventario inicializado en "
                                    + purchase.getSucursal().getNombre()
                    ));

            boolean movimientoExiste = inventoryMovementRepository
                    .existsByProductoIdAndSucursalIdAndTipoMovimientoAndTipoReferenciaAndReferenciaId(
                            productId,
                            sucursalId,
                            MovementType.ENTRY,
                            TIPO_REFERENCIA_COMPRA,
                            purchaseId
                    );

            if (movimientoExiste) {
                throw new IllegalStateException(
                        "Ya existe un movimiento de recepción para el producto "
                                + product.getSku() + " en esta compra"
                );
            }

            BigDecimal stockAnterior = productStock.getStock() != null
                    ? productStock.getStock()
                    : BigDecimal.ZERO;
            BigDecimal stockNuevo = stockAnterior.add(cantidadEntrada);

            if (stockNuevo.compareTo(MAX_CANTIDAD) > 0) {
                throw new IllegalArgumentException(
                        "La recepción supera el límite de stock permitido para "
                                + product.getSku()
                );
            }

            productStock.setStock(stockNuevo);
            productStockRepository.save(productStock);

            InventoryMovement movement = InventoryMovement.builder()
                    .producto(product)
                    .sucursal(purchase.getSucursal())
                    .tipoMovimiento(MovementType.ENTRY)
                    .cantidad(cantidadEntrada)
                    .stockAnterior(stockAnterior)
                    .stockNuevo(stockNuevo)
                    .motivo("Recepción de compra #" + purchaseId)
                    .tipoReferencia(TIPO_REFERENCIA_COMPRA)
                    .referenciaId(purchaseId)
                    .usuario(usuario)
                    .build();

            inventoryMovementRepository.save(movement);
        }

        purchase.setStatus(PurchaseStatus.RECEIVED);
        purchase.setReceivedBy(usuario);
        purchase.setReceivedAt(LocalDateTime.now());

        Purchase saved = purchaseRepository.save(purchase);
        return toResponse(saved);
    }

    private void validarSucursalAutorizada(Long sucursalId, User usuario) {
        if (usuario.getRole() == Role.ADMIN) {
            return;
        }

        if (usuario.getSucursal() == null
                || !usuario.getSucursal().getId().equals(sucursalId)) {
            throw new IllegalArgumentException(
                    "No tienes permiso para recibir compras de otra sucursal"
            );
        }
    }

    private PurchaseItem construirDetalle(
            Purchase purchase,
            PurchaseItemRequest request
    ) {
        if (request == null) {
            throw new IllegalArgumentException(
                    "La compra contiene un detalle vacío"
            );
        }

        validarCantidadYPrecio(request);

        Product product = productRepository.findById(request.productId())
                .orElseThrow(() -> new IllegalArgumentException(
                        "Producto no encontrado: " + request.productId()
                ));

        if (!Boolean.TRUE.equals(product.getActive())) {
            throw new IllegalArgumentException(
                    "No se pueden comprar productos inactivos: " + product.getSku()
            );
        }

        ProductUnit productUnit = null;
        BigDecimal conversionFactor = FACTOR_UNIDAD_BASE;
        String unitCode;
        String unitName;

        if (request.productUnitId() != null) {
            if (request.productUnitId() <= 0) {
                throw new IllegalArgumentException(
                        "El ID de la presentación debe ser válido"
                );
            }

            productUnit = productUnitRepository
                    .findByIdAndProductId(request.productUnitId(), product.getId())
                    .orElseThrow(() -> new IllegalArgumentException(
                            "La presentación no existe para el producto " + product.getSku()
                    ));

            if (!Boolean.TRUE.equals(productUnit.getActive())) {
                throw new IllegalArgumentException(
                        "La presentación seleccionada está inactiva"
                );
            }

            if (!Boolean.TRUE.equals(productUnit.getPurchaseAllowed())) {
                throw new IllegalArgumentException(
                        "La presentación seleccionada no está habilitada para compras"
                );
            }

            conversionFactor = productUnit.getConversionFactor();
            unitCode = productUnit.getCode();
            unitName = productUnit.getName();
        } else {
            String baseUnit = product.getUnit();
            unitCode = baseUnit != null && !baseUnit.isBlank()
                    ? baseUnit.trim()
                    : "UNIDAD";
            unitName = unitCode;
        }

        if (conversionFactor == null
                || conversionFactor.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException(
                    "El factor de conversión debe ser mayor que cero para " + product.getSku()
            );
        }

        BigDecimal quantity = request.quantity().setScale(4, RoundingMode.UNNECESSARY);
        BigDecimal quantityBaseUnit = quantity
                .multiply(conversionFactor)
                .setScale(4, RoundingMode.HALF_UP);

        if (quantityBaseUnit.compareTo(BigDecimal.ZERO) <= 0
                || quantityBaseUnit.compareTo(MAX_CANTIDAD) > 0) {
            throw new IllegalArgumentException(
                    "La cantidad convertida supera el límite permitido para " + product.getSku()
            );
        }

        BigDecimal unitPrice = request.unitPurchasePrice()
                .setScale(2, RoundingMode.UNNECESSARY);
        BigDecimal subtotal = quantity.multiply(unitPrice)
                .setScale(2, RoundingMode.HALF_UP);

        return PurchaseItem.builder()
                .purchase(purchase)
                .product(product)
                .productUnit(productUnit)
                .productSkuSnapshot(product.getSku())
                .productNameSnapshot(product.getName())
                .unitCodeSnapshot(unitCode)
                .unitNameSnapshot(unitName)
                .conversionFactorSnapshot(conversionFactor.setScale(4, RoundingMode.HALF_UP))
                .quantity(quantity)
                .quantityBaseUnit(quantityBaseUnit)
                .unitPurchasePrice(unitPrice)
                .subtotal(subtotal)
                .build();
    }

    private void validarCantidadYPrecio(PurchaseItemRequest request) {
        if (request.productId() == null || request.productId() <= 0) {
            throw new IllegalArgumentException("Debe indicar un producto válido");
        }

        if (request.quantity() == null
                || request.quantity().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException(
                    "La cantidad debe ser mayor que cero"
            );
        }

        if (request.quantity().scale() > 4) {
            throw new IllegalArgumentException(
                    "La cantidad admite como máximo cuatro decimales"
            );
        }

        if (request.unitPurchasePrice() == null
                || request.unitPurchasePrice().compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException(
                    "El precio unitario no puede ser negativo"
            );
        }

        if (request.unitPurchasePrice().scale() > 2) {
            throw new IllegalArgumentException(
                    "El precio unitario admite como máximo dos decimales"
            );
        }
    }

    private Long resolverSucursalId(PurchaseRequest request, User usuario) {
        if (usuario.getRole() == Role.ADMIN) {
            if (request.sucursalId() == null || request.sucursalId() <= 0) {
                throw new IllegalArgumentException(
                        "El administrador debe indicar una sucursal válida"
                );
            }
            return request.sucursalId();
        }

        if (usuario.getSucursal() == null) {
            throw new IllegalArgumentException(
                    "El usuario no tiene una sucursal asignada"
            );
        }

        // Los usuarios no administradores siempre registran compras en su sucursal.
        return usuario.getSucursal().getId();
    }

    private void validarUsuario(User usuario) {
        if (usuario == null) {
            throw new IllegalArgumentException(
                    "No se pudo identificar al usuario autenticado"
            );
        }

        if (!usuario.isActive()) {
            throw new IllegalArgumentException(
                    "El usuario se encuentra inactivo"
            );
        }
    }

    private String normalizarTexto(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private PurchaseResponse toResponse(Purchase purchase) {
        List<PurchaseItemResponse> items = purchase.getItems().stream()
                .map(this::toItemResponse)
                .toList();

        return new PurchaseResponse(
                purchase.getId(),
                purchase.getSupplier().getId(),
                purchase.getSupplier().getNombre(),
                purchase.getSucursal().getId(),
                purchase.getDocumentType(),
                purchase.getDocumentNumber(),
                purchase.getPurchaseDate(),
                purchase.getStatus(),
                purchase.getTotal(),
                purchase.getNotes(),
                purchase.getCreatedBy() != null ? purchase.getCreatedBy().getId() : null,
                purchase.getReceivedBy() != null ? purchase.getReceivedBy().getId() : null,
                purchase.getReceivedAt(),
                purchase.getCreatedAt(),
                purchase.getUpdatedAt(),
                items
        );
    }

    private PurchaseItemResponse toItemResponse(PurchaseItem item) {
        return new PurchaseItemResponse(
                item.getId(),
                item.getProduct().getId(),
                item.getProductUnit() != null ? item.getProductUnit().getId() : null,
                item.getProductSkuSnapshot(),
                item.getProductNameSnapshot(),
                item.getUnitCodeSnapshot(),
                item.getUnitNameSnapshot(),
                item.getConversionFactorSnapshot(),
                item.getQuantity(),
                item.getQuantityBaseUnit(),
                item.getUnitPurchasePrice(),
                item.getSubtotal()
        );
    }
}
