package com.ferreteria.fc.domain.service.Impl;

import com.ferreteria.fc.domain.dto.mapper.OrderMapper;
import com.ferreteria.fc.domain.dto.request.InventoryMovementRequest;
import com.ferreteria.fc.domain.dto.request.CreateOrderRequest;
import com.ferreteria.fc.domain.dto.request.OrderItemRequest;
import com.ferreteria.fc.domain.dto.request.UpdateOrderRequest;
import com.ferreteria.fc.domain.dto.request.UpdatePatchOrderRequest;
import com.ferreteria.fc.domain.dto.response.OrderResponse;
import com.ferreteria.fc.domain.dto.response.OrderSummaryResponse;
import com.ferreteria.fc.domain.dto.utils.WazeUtils;
import com.ferreteria.fc.domain.enums.OrderStatus;
import com.ferreteria.fc.domain.enums.MovementType;
import com.ferreteria.fc.domain.enums.PaymentStatus;
import com.ferreteria.fc.domain.enums.Role;
import com.ferreteria.fc.domain.repository.OrderRepository;
import com.ferreteria.fc.domain.repository.InventoryMovementRepository;
import com.ferreteria.fc.domain.repository.ProductRepository;
import com.ferreteria.fc.domain.repository.ProductUnitRepository;
import com.ferreteria.fc.domain.repository.StoreRepository;
import com.ferreteria.fc.domain.service.OrderService;
import com.ferreteria.fc.domain.service.InventoryService;
import com.ferreteria.fc.persistence.entity.Order;
import com.ferreteria.fc.persistence.entity.OrderItem;
import com.ferreteria.fc.persistence.entity.Product;
import com.ferreteria.fc.persistence.entity.ProductUnit;
import com.ferreteria.fc.persistence.entity.Store;
import com.ferreteria.fc.persistence.entity.User;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.Map;
import java.util.LinkedHashMap;

@Service
@Transactional
public class OrderServiceImpl implements OrderService {

    private final OrderRepository orderRepository;
    private final ProductRepository productRepository;
    private final ProductUnitRepository productUnitRepository;
    private final OrderMapper orderMapper;
    private final StoreRepository storeRepository;
    private final InventoryService inventoryService;
    private final InventoryMovementRepository inventoryMovementRepository;

    public OrderServiceImpl(
            OrderRepository orderRepository,
            ProductRepository productRepository,
            ProductUnitRepository productUnitRepository,
            OrderMapper orderMapper,
            StoreRepository storeRepository,
            InventoryService inventoryService,
            InventoryMovementRepository inventoryMovementRepository
    ) {
        this.orderRepository = orderRepository;
        this.productRepository = productRepository;
        this.productUnitRepository = productUnitRepository;
        this.orderMapper = orderMapper;
        this.storeRepository = storeRepository;
        this.inventoryService = inventoryService;
        this.inventoryMovementRepository = inventoryMovementRepository;
    }

    @Override
    public OrderResponse createOrder(CreateOrderRequest request, User user) {
        Order order = new Order();
        order.setOrderNumber(generateOrderNumber(orderRepository.nextOrderNumberValue()));
        order.setCustomerName(request.getCustomerName());
        order.setPhone(request.getPhone());
        order.setAddress(request.getAddress());
        order.setPaymentMethod(request.getPaymentMethod());
        order.setPaymentStatus(PaymentStatus.PENDING);
        order.setStatus(OrderStatus.CREATED);
        order.setStatusChangedAt(LocalDateTime.now());
        order.setNotes(request.getNotes());
        order.setCreatedBy(user);
        order.setDeliveryDate(request.getDeliveryDate());

        Store store = storeRepository.findById(request.getStoreId())
                .orElseThrow(() -> new IllegalArgumentException(
                        "Sucursal no encontrada: " + request.getStoreId()
                ));

        // Los administradores pueden seleccionar cualquier sucursal activa.
        // Los demás usuarios solo pueden crear pedidos en su sucursal asignada.
        if (user.getRole() != Role.ADMIN
                && (user.getSucursal() == null
                || !Objects.equals(user.getSucursal().getId(), store.getId()))) {
            throw new IllegalArgumentException(
                    "No tienes permiso para crear pedidos en esta sucursal"
            );
        }

        if (!Boolean.TRUE.equals(store.getActiva())) {
            throw new IllegalArgumentException(
                    "La sucursal seleccionada está inactiva"
            );
        }

        order.setStore(store);

        List<OrderItem> items = request.getItems().stream()
                .map(item -> buildItem(item, order)).toList();

        order.setItems(items);
        applyWholesalePricing(items);
        recalculateTotal(order);
        order.setWazeLink(WazeUtils.generateLink(order.getAddress()));
        return orderMapper.toResponse(orderRepository.save(order));
    }

    private String generateOrderNumber(Long sequenceValue) {
        long group = ((sequenceValue - 1) / 9999) + 1;
        long consecutive = ((sequenceValue - 1) % 9999) + 1;

        StringBuilder letters = new StringBuilder();
        while (group > 0) {
            group--;
            letters.insert(0, (char) ('A' + (group % 26)));
            group /= 26;
        }

        return letters + String.format("%04d", consecutive);
    }

    private OrderItem buildItem(OrderItemRequest request, Order order) {
        if (request.getQuantity() == null
                || request.getQuantity().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException(
                    "La cantidad del producto debe ser mayor que cero"
            );
        }

        if (request.getUnitId() != null && request.getProductId() == null) {
            throw new IllegalArgumentException(
                    "Debe indicar un producto del catálogo para seleccionar una presentación"
            );
        }

        Product product = null;
        ProductUnit selectedUnit = null;
        String productName = request.getProductName();
        BigDecimal unitPrice = request.getUnitPrice();
        boolean wholesale = false;
        String pricingType = "UNIT";

        if (request.getProductId() != null) {
            product = productRepository.findById(request.getProductId())
                    .orElseThrow(() -> new IllegalArgumentException(
                            "Producto no encontrado: " + request.getProductId()
                    ));

            if (!Boolean.TRUE.equals(product.getActive())) {
                throw new IllegalArgumentException(
                        "El producto está inactivo: " + product.getName()
                );
            }

            if (product.getBaseUnit() == null) {
                throw new IllegalArgumentException("Debes definir la unidad base del producto " + product.getName() + " en Productos → Presentaciones antes de venderlo.");
            }

            productName = product.getName();
            unitPrice = product.getPrice();

            if (request.getUnitId() != null) {
                selectedUnit = productUnitRepository
                        .findByIdAndProductId(request.getUnitId(), product.getId())
                        .orElseThrow(() -> new IllegalArgumentException(
                                "La presentación no pertenece al producto seleccionado"
                        ));

                if (!Boolean.TRUE.equals(selectedUnit.getActive())
                        || !Boolean.TRUE.equals(selectedUnit.getSaleAllowed())) {
                    throw new IllegalArgumentException(
                            "La presentación seleccionada no está habilitada para la venta"
                    );
                }

                if (selectedUnit.getConversionFactor() == null
                        || selectedUnit.getConversionFactor().compareTo(BigDecimal.ZERO) <= 0) {
                    throw new IllegalArgumentException(
                            "La presentación tiene un factor de conversión inválido"
                    );
                }
            }

            BigDecimal conversionFactor = selectedUnit != null
                    ? selectedUnit.getConversionFactor()
                    : BigDecimal.ONE;
            BigDecimal quantityInBaseUnit = request.getQuantity().multiply(conversionFactor);
            BigDecimal minimum = product.getMinimumSaleQuantity() != null ? product.getMinimumSaleQuantity() : BigDecimal.ONE;
            BigDecimal increment = product.getSaleQuantityIncrement() != null ? product.getSaleQuantityIncrement() : BigDecimal.ONE;
            if (quantityInBaseUnit.compareTo(minimum) < 0) {
                throw new IllegalArgumentException("La cantidad mínima de venta para " + product.getName() + " es " + minimum + " " + product.getBaseUnit().getSymbol() + ".");
            }
            if (increment.compareTo(BigDecimal.ZERO) <= 0
                    || quantityInBaseUnit.subtract(minimum).remainder(increment).compareTo(BigDecimal.ZERO) != 0) {
                throw new IllegalArgumentException("La cantidad debe respetar incrementos de " + increment + " " + product.getBaseUnit().getSymbol() + ", desde un mínimo de " + minimum + ".");
            }

            // El precio mayorista se aplica después de construir todas las líneas,
            // considerando la cantidad total del producto en su unidad base.
        }

        if (productName == null || productName.isBlank()) {
            throw new IllegalArgumentException(
                    "Debe indicar un producto o productoId"
            );
        }

        if (unitPrice == null || unitPrice.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException(
                    "No existe un precio válido para el producto " + productName
            );
        }

        BigDecimal conversionFactor = selectedUnit != null
                ? selectedUnit.getConversionFactor()
                : BigDecimal.ONE;

        OrderItem item = new OrderItem();
        item.setOrder(order);
        item.setProduct(product);
        item.setProductName(productName);
        item.setQuantity(request.getQuantity());
        item.setUnitIdSnapshot(selectedUnit != null ? selectedUnit.getId() : null);
        item.setUnitCodeSnapshot(selectedUnit != null
                ? selectedUnit.getCode()
                : (product != null ? "BASE" : "CUSTOM"));
        item.setUnitNameSnapshot(selectedUnit != null
                ? selectedUnit.getName()
                : (product != null && product.getBaseUnit() != null
                    ? product.getBaseUnit().getName()
                    : "Unidad base"));
        item.setConversionFactorSnapshot(conversionFactor);
        item.setUnitPrice(unitPrice);
        // Product.price y Product.wholesalePrice corresponden al precio por unidad base.
        // El factor solo convierte la cantidad; no crea un precio distinto por presentación.
        item.setTotalPrice(unitPrice.multiply(request.getQuantity()).multiply(conversionFactor));
        item.setWholesaleApplied(wholesale);
        item.setPricingType(pricingType);
        return item;
    }

    private void recalculateTotal(Order order) {
        order.setTotalAmount(order.getItems().stream()
                .map(OrderItem::getTotalPrice)
                .reduce(BigDecimal.ZERO, BigDecimal::add));
    }

    private void applyWholesalePricing(List<OrderItem> items) {
        Map<Long, List<OrderItem>> itemsByProduct = new LinkedHashMap<>();
        for (OrderItem item : items) {
            if (item.getProduct() != null) {
                itemsByProduct.computeIfAbsent(item.getProduct().getId(), key -> new java.util.ArrayList<>())
                        .add(item);
            }
        }

        for (List<OrderItem> productItems : itemsByProduct.values()) {
            Product product = productItems.get(0).getProduct();
            BigDecimal totalInBaseUnit = productItems.stream()
                    .map(item -> item.getQuantity().multiply(
                            item.getConversionFactorSnapshot() != null
                                    ? item.getConversionFactorSnapshot()
                                    : BigDecimal.ONE))
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            boolean wholesale = product.getWholesalePrice() != null
                    && product.getWholesaleMinQuantity() != null
                    && totalInBaseUnit.compareTo(product.getWholesaleMinQuantity()) >= 0;
            BigDecimal price = wholesale ? product.getWholesalePrice() : product.getPrice();

            for (OrderItem item : productItems) {
                BigDecimal factor = item.getConversionFactorSnapshot() != null
                        ? item.getConversionFactorSnapshot()
                        : BigDecimal.ONE;
                item.setUnitPrice(price);
                item.setTotalPrice(price.multiply(item.getQuantity()).multiply(factor));
                item.setWholesaleApplied(wholesale);
                item.setPricingType(wholesale ? "WHOLESALE" : "UNIT");
            }
        }
    }

    private void synchronizeInventoryForOrder(Order order, User user) {
        if (order.getId() == null || order.getStore() == null || order.getItems() == null) {
            return;
        }

        boolean shouldExit = order.getStatus() == OrderStatus.IN_ROUTE
                || order.getStatus() == OrderStatus.DELIVERED;
        boolean shouldReturn = order.getStatus() == OrderStatus.CANCELED
                || order.getStatus() == OrderStatus.REJECTED;
        if (!shouldExit && !shouldReturn) return;

        Map<Long, BigDecimal> quantities = new LinkedHashMap<>();
        for (OrderItem item : order.getItems()) {
            if (item.getProduct() == null || item.getQuantity() == null) continue;
            Long productId = item.getProduct().getId();
            BigDecimal factor = item.getConversionFactorSnapshot() != null
                    ? item.getConversionFactorSnapshot()
                    : BigDecimal.ONE;
            quantities.merge(productId, item.getQuantity().multiply(factor), BigDecimal::add);
        }

        for (Map.Entry<Long, BigDecimal> entry : quantities.entrySet()) {
            Long productId = entry.getKey();
            BigDecimal quantity = entry.getValue();
            boolean exitExists = inventoryMovementRepository
                    .existsByProductoIdAndSucursalIdAndTipoMovimientoAndTipoReferenciaAndReferenciaId(
                            productId, order.getStore().getId(), MovementType.EXIT, "ORDER", order.getId());
            boolean returnExists = inventoryMovementRepository
                    .existsByProductoIdAndSucursalIdAndTipoMovimientoAndTipoReferenciaAndReferenciaId(
                            productId, order.getStore().getId(), MovementType.ENTRY, "ORDER_RETURN", order.getId());

            InventoryMovementRequest movement = InventoryMovementRequest.builder()
                    .productoId(productId)
                    .sucursalId(order.getStore().getId())
                    .cantidad(quantity)
                    .motivo("Pedido " + order.getOrderNumber())
                    .tipoReferencia("ORDER")
                    .referenciaId(order.getId())
                    .build();

            if (shouldExit && !exitExists) {
                inventoryService.registerExit(movement, user);
            } else if (shouldReturn && exitExists && !returnExists) {
                movement.setTipoReferencia("ORDER_RETURN");
                movement.setMotivo("Devolución de pedido " + order.getOrderNumber());
                inventoryService.registerEntry(movement, user);
            }
        }
    }

    private void validateStatusTransition(OrderStatus current, OrderStatus next) {
        if (current == OrderStatus.IN_ROUTE
                && (next == OrderStatus.CREATED || next == OrderStatus.PREPARING)) {
            throw new IllegalStateException("Un pedido en ruta no puede volver a preparación ni a creado.");
        }
        if ((current == OrderStatus.CANCELED || current == OrderStatus.REJECTED)
                && next != current) {
            throw new IllegalStateException("Un pedido cancelado o rechazado no puede reabrirse.");
        }
        if (current == OrderStatus.DELIVERED
                && next != OrderStatus.CANCELED && next != OrderStatus.REJECTED) {
            throw new IllegalStateException("Un pedido entregado solo puede pasar a cancelado o rechazado mediante reversa.");
        }
    }


    public List<OrderResponse> getAll() {
        return orderMapper.toListResponse(orderRepository.findAll());
    }

    public OrderResponse getById(Long id) {
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new RuntimeException(
                        "Pedido no encontrado: " + id
                ));
        return orderMapper.toResponse(order);
    }

    @Override
    public OrderResponse updateOrder(Long id, UpdateOrderRequest request, User user) {
        Order order = orderRepository.findByIdForUpdate(id)
                .orElseThrow(() -> new RuntimeException("Orden no encontrada"));

        order.setCustomerName(request.getCustomerName());
        order.setPhone(request.getPhone());
        order.setAddress(request.getAddress());
        order.setDeliveryDate(request.getDeliveryDate());

        if (request.getPaymentMethod() != null
                && order.getPaymentStatus() != PaymentStatus.PENDING
                && request.getPaymentMethod() != order.getPaymentMethod()) {
            throw new IllegalArgumentException(
                    "No se puede cambiar el método de pago después de registrar el pago"
            );
        }
        if (request.getPaymentMethod() != null) {
            order.setPaymentMethod(request.getPaymentMethod());
        }

        if (request.getStatus() != null && request.getStatus() != order.getStatus()) {
            validateStatusTransition(order.getStatus(), request.getStatus());
            if (request.getStatus() == OrderStatus.DELIVERED
                    && order.getPaymentStatus() != PaymentStatus.PAID) {
                throw new IllegalStateException(
                        "Para marcar el pedido como entregado, primero debes registrar el pago como recibido"
                );
            }
            order.setStatus(request.getStatus());
            order.setStatusChangedAt(LocalDateTime.now());
            reversePaymentIfRequired(order, user, request.getStatus());
        }

        order.setNotes(request.getNotes());
        order.setWazeLink(WazeUtils.generateLink(request.getAddress()));

        if ((order.getStatus() == OrderStatus.IN_ROUTE || order.getStatus() == OrderStatus.DELIVERED)
                && request.getItems() != null && !sameItems(order, request.getItems())) {
            throw new IllegalStateException("No se pueden modificar productos ni cantidades después de descontar el stock al poner el pedido en ruta.");
        }

        if (order.getPaymentStatus() != PaymentStatus.PENDING
                && request.getItems() != null
                && !sameItems(order, request.getItems())) {
            throw new IllegalStateException(
                    "No se pueden modificar los productos ni el total después de registrar o reversar el pago"
            );
        }

        // Si las líneas del pedido no cambiaron, se conservan tal cual.
        // Esto permite avanzar a EN_RUTA pedidos históricos que contienen
        // productos posteriormente desactivados, sin reconstruirlos ni revalidarlos.
        if (request.getItems() != null && !sameItems(order, request.getItems())) {
            order.getItems().clear();
            List<OrderItem> items = request.getItems().stream()
                    .map(item -> buildItem(item, order)).toList();
            order.getItems().addAll(items);
            applyWholesalePricing(items);
            recalculateTotal(order);
        }

        synchronizeInventoryForOrder(order, user);
        return orderMapper.toResponse(orderRepository.save(order));
    }

    private boolean sameItems(Order order, List<OrderItemRequest> requestedItems) {
        if (order.getItems() == null || requestedItems == null
                || order.getItems().size() != requestedItems.size()) {
            return false;
        }

        for (int i = 0; i < order.getItems().size(); i++) {
            OrderItem current = order.getItems().get(i);
            OrderItemRequest requested = requestedItems.get(i);

            if (!Objects.equals(
                    current.getProduct() != null ? current.getProduct().getId() : null,
                    requested.getProductId())
                    || !Objects.equals(current.getUnitIdSnapshot(), requested.getUnitId())
                    || current.getQuantity() == null
                    || requested.getQuantity() == null
                    || current.getQuantity().compareTo(requested.getQuantity()) != 0) {
                return false;
            }

            if (requested.getProductId() == null
                    && !Objects.equals(current.getProductName(), requested.getProductName())) {
                return false;
            }
        }
        return true;
    }

    public OrderResponse patchOrder(Long id, UpdatePatchOrderRequest request, User user) {
        Order order = orderRepository.findByIdForUpdate(id)
                .orElseThrow(() -> new RuntimeException("Pedido no encontrado"));

        if (request.getDeliveryDate() != null) order.setDeliveryDate(request.getDeliveryDate());
        if (request.getCustomerName() != null) order.setCustomerName(request.getCustomerName());
        if (request.getPhone() != null) order.setPhone(request.getPhone());
        if (request.getAddress() != null) {
            order.setAddress(request.getAddress());
            order.setWazeLink(WazeUtils.generateLink(request.getAddress()));
        }
        if (request.getPaymentMethod() != null
                && order.getPaymentStatus() != PaymentStatus.PENDING
                && request.getPaymentMethod() != order.getPaymentMethod()) {
            throw new IllegalArgumentException(
                    "No se puede cambiar el método de pago después de registrar el pago"
            );
        }
        if (request.getPaymentMethod() != null) order.setPaymentMethod(request.getPaymentMethod());
        if (request.getNotes() != null) order.setNotes(request.getNotes());

        if (request.getStatus() != null && request.getStatus() != order.getStatus()) {
            validateStatusTransition(order.getStatus(), request.getStatus());
            if (request.getStatus() == OrderStatus.DELIVERED
                    && order.getPaymentStatus() != PaymentStatus.PAID) {
                throw new IllegalStateException(
                        "Para marcar el pedido como entregado, primero debes registrar el pago como recibido"
                );
            }
            order.setStatus(request.getStatus());
            order.setStatusChangedAt(LocalDateTime.now());
            reversePaymentIfRequired(order, user, request.getStatus());
        }

        synchronizeInventoryForOrder(order, user);
        return orderMapper.toResponse(orderRepository.save(order));
    }
    @Override
    public OrderResponse confirmPayment(Long id, User user) {
        Order order = orderRepository.findByIdForUpdate(id)
                .orElseThrow(() -> new RuntimeException("Pedido no encontrado: " + id));

        if (order.getItems() == null || order.getItems().isEmpty()) {
            throw new IllegalStateException("Agrega al menos un producto antes de confirmar el pago");
        }

        if (order.getPaymentStatus() != PaymentStatus.PENDING) {
            throw new IllegalStateException("El pago del pedido ya fue procesado");
        }

        if (order.getStatus() == OrderStatus.CANCELED
                || order.getStatus() == OrderStatus.REJECTED) {
            throw new IllegalStateException("No se puede confirmar el pago de un pedido cancelado o rechazado");
        }

        if (order.getTotalAmount() == null
                || order.getTotalAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalStateException("El total del pedido debe ser mayor que cero para confirmar el pago");
        }

        order.setPaymentStatus(PaymentStatus.PAID);
        order.setPaymentReceivedAt(LocalDateTime.now());
        order.setPaymentReceivedBy(user);

        return orderMapper.toResponse(orderRepository.save(order));
    }

    private void reversePaymentIfRequired(Order order, User user, OrderStatus newStatus) {
        boolean terminalStatus = newStatus == OrderStatus.CANCELED
                || newStatus == OrderStatus.REJECTED;

        if (terminalStatus && order.getPaymentStatus() == PaymentStatus.PAID) {
            order.setPaymentStatus(PaymentStatus.REVERSED);
            order.setPaymentReversedAt(LocalDateTime.now());
            order.setPaymentReversedBy(user);
            order.setPaymentReversalReason(newStatus.name());
        }
    }

    @Override
    public List<OrderSummaryResponse> findLatestOrders(Long storeId) {
        return orderRepository.findLatestOrders(storeId, PageRequest.of(0, 10));
    }

    @Override
    public List<OrderSummaryResponse> findAllSummary(Long storeId) {
        return orderRepository.findAllOrders(storeId);
    }
}
