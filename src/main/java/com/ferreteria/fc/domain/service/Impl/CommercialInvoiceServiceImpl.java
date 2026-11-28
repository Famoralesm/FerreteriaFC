package com.ferreteria.fc.domain.service.Impl;

import com.ferreteria.fc.domain.dto.request.CreateCommercialInvoiceRequest;
import com.ferreteria.fc.domain.dto.request.CommercialInvoiceItemRequest;
import com.ferreteria.fc.domain.dto.response.CommercialInvoiceItemResponse;
import com.ferreteria.fc.domain.dto.response.CommercialInvoiceResponse;
import com.ferreteria.fc.domain.enums.CommercialInvoiceStatus;
import com.ferreteria.fc.domain.enums.InvoiceTaxStatus;
import com.ferreteria.fc.domain.enums.OrderStatus;
import com.ferreteria.fc.domain.enums.Role;
import com.ferreteria.fc.domain.repository.CommercialInvoiceRepository;
import com.ferreteria.fc.domain.repository.OrderRepository;
import com.ferreteria.fc.domain.repository.ProductRepository;
import com.ferreteria.fc.domain.repository.StoreRepository;
import com.ferreteria.fc.domain.service.AuditLogService;
import com.ferreteria.fc.domain.service.CommercialInvoiceService;
import com.ferreteria.fc.persistence.entity.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Service
@Transactional
public class CommercialInvoiceServiceImpl implements CommercialInvoiceService {
    private final CommercialInvoiceRepository invoiceRepository;
    private final OrderRepository orderRepository;
    private final StoreRepository storeRepository;
    private final ProductRepository productRepository;
    private final AuditLogService auditLogService;

    public CommercialInvoiceServiceImpl(
            CommercialInvoiceRepository invoiceRepository,
            OrderRepository orderRepository,
            StoreRepository storeRepository,
            ProductRepository productRepository,
            AuditLogService auditLogService
    ) {
        this.invoiceRepository = invoiceRepository;
        this.orderRepository = orderRepository;
        this.storeRepository = storeRepository;
        this.productRepository = productRepository;
        this.auditLogService = auditLogService;
    }

    @Override
    public CommercialInvoiceResponse create(CreateCommercialInvoiceRequest request, User user) {
        Store store = storeRepository.findById(request.getStoreId())
                .orElseThrow(() -> new IllegalArgumentException("Sucursal no encontrada."));
        validateStoreAccess(store, user);

        CommercialInvoice invoice = new CommercialInvoice();
        invoice.setStore(store);
        invoice.setCreatedBy(user);
        invoice.setCustomerName(request.getCustomerName().trim());
        invoice.setCustomerRut(clean(request.getCustomerRut()));
        invoice.setCustomerBusinessName(clean(request.getCustomerBusinessName()));
        invoice.setCustomerGiro(clean(request.getCustomerGiro()));
        invoice.setCustomerAddress(clean(request.getCustomerAddress()));
        invoice.setCustomerCity(clean(request.getCustomerCity()));
        invoice.setCustomerCommune(clean(request.getCustomerCommune()));
        invoice.setCustomerEmail(clean(request.getCustomerEmail()));
        invoice.setCustomerPhone(clean(request.getCustomerPhone()));
        invoice.setNotes(clean(request.getNotes()));
        invoice.setStatus(CommercialInvoiceStatus.REGISTERED);
        invoice.setTaxStatus(InvoiceTaxStatus.NOT_SENT);

        for (CommercialInvoiceItemRequest line : request.getItems()) {
            if (line.getQuantity().signum() <= 0 || line.getUnitPrice().signum() < 0) {
                throw new IllegalArgumentException("La cantidad debe ser positiva y el precio no puede ser negativo.");
            }
            Product product = null;
            String productName = line.getProductName().trim();
            if (line.getProductId() != null) {
                product = productRepository.findById(line.getProductId())
                        .orElseThrow(() -> new IllegalArgumentException("Producto no encontrado: " + line.getProductId()));
                if (!Boolean.TRUE.equals(product.getActive())) {
                    throw new IllegalArgumentException("No se puede facturar un producto inactivo: " + product.getName());
                }
                productName = product.getName();
            }
            BigDecimal lineTotal = line.getQuantity().multiply(line.getUnitPrice()).setScale(2, java.math.RoundingMode.HALF_UP);
            invoice.getItems().add(CommercialInvoiceItem.builder()
                    .invoice(invoice)
                    .product(product)
                    .productName(productName)
                    .quantity(line.getQuantity())
                    .unitName(clean(line.getUnitName()))
                    .unitPrice(line.getUnitPrice())
                    .lineTotal(lineTotal)
                    .build());
        }
        persistTotalsAndNumber(invoice);
        auditLogService.record("INVOICES", "COMMERCIAL_INVOICE", invoice.getId().toString(),
                "CREATED", user, store, null, auditValues(invoice),
                "Registro comercial " + invoice.getInternalNumber() + " creado", "WEB");
        return toResponse(invoice);
    }

    @Override
    public CommercialInvoiceResponse createFromOrder(Long orderId, User user) {
        Order order = orderRepository.findByIdForUpdate(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Pedido no encontrado."));
        validateStoreAccess(order.getStore(), user);
        if (order.getStatus() == OrderStatus.CANCELED || order.getStatus() == OrderStatus.REJECTED) {
            throw new IllegalStateException("No se puede generar una factura desde un pedido cancelado o rechazado.");
        }
        if (invoiceRepository.existsBySourceOrder_Id(orderId)) {
            throw new IllegalStateException("Este pedido ya tiene un registro de factura asociado.");
        }

        CommercialInvoice invoice = new CommercialInvoice();
        invoice.setStore(order.getStore());
        invoice.setSourceOrder(order);
        invoice.setCreatedBy(user);
        invoice.setCustomerName(order.getCustomerName());
        invoice.setCustomerAddress(order.getAddress());
        invoice.setCustomerPhone(order.getPhone());
        invoice.setNotes("Generada desde el pedido " + order.getOrderNumber());
        invoice.setStatus(CommercialInvoiceStatus.REGISTERED);
        invoice.setTaxStatus(InvoiceTaxStatus.NOT_SENT);

        for (OrderItem source : order.getItems()) {
            invoice.getItems().add(CommercialInvoiceItem.builder()
                    .invoice(invoice)
                    .product(source.getProduct())
                    .productName(source.getProductName())
                    .quantity(source.getQuantity())
                    .unitName(source.getUnitNameSnapshot())
                    .unitPrice(source.getUnitPrice())
                    .lineTotal(source.getTotalPrice())
                    .build());
        }
        persistTotalsAndNumber(invoice);
        auditLogService.record("INVOICES", "COMMERCIAL_INVOICE", invoice.getId().toString(),
                "CREATED_FROM_ORDER", user, order.getStore(), null, auditValues(invoice),
                "Registro comercial " + invoice.getInternalNumber() + " generado desde pedido " + order.getOrderNumber(), "WEB");
        return toResponse(invoice);
    }

    @Override
    @Transactional(readOnly = true)
    public List<CommercialInvoiceResponse> findAll(Long storeId, User user) {
        List<CommercialInvoice> invoices;
        if (user.getRole() == Role.ADMIN && storeId == null) {
            invoices = invoiceRepository.findAllByOrderByCreatedAtDesc();
        } else {
            Long effectiveStoreId = storeId != null ? storeId
                    : user.getSucursal() != null ? user.getSucursal().getId() : null;
            if (effectiveStoreId == null) throw new IllegalArgumentException("Debes seleccionar una sucursal.");
            Store store = storeRepository.findById(effectiveStoreId)
                    .orElseThrow(() -> new IllegalArgumentException("Sucursal no encontrada."));
            validateStoreAccess(store, user);
            invoices = invoiceRepository.findAllByStore_IdOrderByCreatedAtDesc(effectiveStoreId);
        }
        return invoices.stream().map(this::toResponse).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public CommercialInvoiceResponse findById(Long id, User user) {
        CommercialInvoice invoice = invoiceRepository.findDetailedById(id)
                .orElseThrow(() -> new IllegalArgumentException("Registro de factura no encontrado."));
        validateStoreAccess(invoice.getStore(), user);
        return toResponse(invoice);
    }

    @Override
    public CommercialInvoiceResponse cancel(Long id, User user) {
        CommercialInvoice invoice = invoiceRepository.findDetailedById(id)
                .orElseThrow(() -> new IllegalArgumentException("Registro de factura no encontrado."));
        validateStoreAccess(invoice.getStore(), user);
        if (invoice.getStatus() == CommercialInvoiceStatus.CANCELED) {
            throw new IllegalStateException("El registro ya está anulado.");
        }
        if (invoice.getTaxStatus() != InvoiceTaxStatus.NOT_SENT) {
            throw new IllegalStateException("No se puede anular desde FerreteríaFC un documento que tenga gestión tributaria iniciada.");
        }
        invoice.setStatus(CommercialInvoiceStatus.CANCELED);
        invoice.setCanceledAt(LocalDateTime.now());
        CommercialInvoice saved = invoiceRepository.save(invoice);
        auditLogService.record("INVOICES", "COMMERCIAL_INVOICE", saved.getId().toString(),
                "CANCELED", user, saved.getStore(),
                Map.of("status", CommercialInvoiceStatus.REGISTERED.name()),
                Map.of("status", CommercialInvoiceStatus.CANCELED.name()),
                "Registro comercial " + saved.getInternalNumber() + " anulado", "WEB");
        return toResponse(saved);
    }

    private void persistTotalsAndNumber(CommercialInvoice invoice) {
        BigDecimal total = invoice.getItems().stream().map(CommercialInvoiceItem::getLineTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add).setScale(2, java.math.RoundingMode.HALF_UP);
        invoice.setSubtotal(total);
        invoice.setTotalAmount(total);
        CommercialInvoice saved = invoiceRepository.saveAndFlush(invoice);
        saved.setInternalNumber("FC-" + String.format("%08d", saved.getId()));
    }

    private void validateStoreAccess(Store store, User user) {
        if (store == null || !Boolean.TRUE.equals(store.getActiva())) {
            throw new IllegalArgumentException("La sucursal no está disponible.");
        }
        if (user.getRole() != Role.ADMIN
                && (user.getSucursal() == null || !Objects.equals(user.getSucursal().getId(), store.getId()))) {
            throw new IllegalArgumentException("No tienes permiso para operar facturas en esta sucursal.");
        }
    }

    private String clean(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private Map<String, Object> auditValues(CommercialInvoice invoice) {
        return Map.of("internalNumber", invoice.getInternalNumber(),
                "customerName", invoice.getCustomerName(),
                "totalAmount", invoice.getTotalAmount(),
                "status", invoice.getStatus().name(),
                "taxStatus", invoice.getTaxStatus().name());
    }

    private CommercialInvoiceResponse toResponse(CommercialInvoice invoice) {
        List<CommercialInvoiceItemResponse> items = invoice.getItems().stream()
                .map(item -> new CommercialInvoiceItemResponse(
                        item.getId(),
                        item.getProduct() != null ? item.getProduct().getId() : null,
                        item.getProductName(),
                        item.getQuantity(),
                        item.getUnitName(),
                        item.getUnitPrice(),
                        item.getLineTotal()
                )).toList();
        return new CommercialInvoiceResponse(
                invoice.getId(), invoice.getInternalNumber(),
                invoice.getStore().getId(), invoice.getStore().getNombre(),
                invoice.getSourceOrder() != null ? invoice.getSourceOrder().getId() : null,
                invoice.getSourceOrder() != null ? invoice.getSourceOrder().getOrderNumber() : null,
                invoice.getCustomerName(), invoice.getCustomerRut(), invoice.getCustomerBusinessName(),
                invoice.getCustomerGiro(), invoice.getCustomerAddress(), invoice.getCustomerCity(),
                invoice.getCustomerCommune(), invoice.getCustomerEmail(), invoice.getCustomerPhone(),
                invoice.getSubtotal(), invoice.getTotalAmount(), invoice.getStatus(), invoice.getTaxStatus(),
                invoice.getCreatedBy() != null ? invoice.getCreatedBy().getName() : null,
                invoice.getNotes(), invoice.getCreatedAt(), invoice.getUpdatedAt(), invoice.getCanceledAt(), items
        );
    }
}
