package com.ferreteria.fc.domain.service.Impl;

import com.ferreteria.fc.domain.dto.purchase.*;
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
import java.util.ArrayList;
import java.util.List;

@Service
@Transactional
public class PurchaseServiceImpl implements PurchaseService {

    private static final BigDecimal FACTOR_UNIDAD_BASE = BigDecimal.ONE;
    private static final BigDecimal MAX_CANTIDAD = new BigDecimal("9999999999.9999");

    private final PurchaseRepository purchaseRepository;
    private final SupplierRepository supplierRepository;
    private final StoreRepository storeRepository;
    private final ProductRepository productRepository;
    private final ProductUnitRepository productUnitRepository;

    public PurchaseServiceImpl(
            PurchaseRepository purchaseRepository,
            SupplierRepository supplierRepository,
            StoreRepository storeRepository,
            ProductRepository productRepository,
            ProductUnitRepository productUnitRepository
    ) {
        this.purchaseRepository = purchaseRepository;
        this.supplierRepository = supplierRepository;
        this.storeRepository = storeRepository;
        this.productRepository = productRepository;
        this.productUnitRepository = productUnitRepository;
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
