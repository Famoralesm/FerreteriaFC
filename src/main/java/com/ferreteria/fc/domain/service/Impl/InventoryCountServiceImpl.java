package com.ferreteria.fc.domain.service.Impl;

import com.ferreteria.fc.domain.dto.request.CreateInventoryCountRequest;
import com.ferreteria.fc.domain.dto.request.InventoryCountLineRequest;
import com.ferreteria.fc.domain.dto.response.*;
import com.ferreteria.fc.domain.enums.*;
import com.ferreteria.fc.domain.repository.*;
import com.ferreteria.fc.domain.service.InventoryCountService;
import com.ferreteria.fc.domain.service.AuditLogService;
import com.ferreteria.fc.persistence.entity.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.List;
import java.math.BigDecimal;
import java.util.Map;

@Service
@Transactional
public class InventoryCountServiceImpl implements InventoryCountService {
    private final InventoryCountRepository countRepository;
    private final InventoryCountDetailRepository detailRepository;
    private final ProductStockRepository stockRepository;
    private final StoreRepository storeRepository;
    private final InventoryMovementRepository movementRepository;
    private final AuditLogService auditLogService;

    public InventoryCountServiceImpl(InventoryCountRepository countRepository,
            InventoryCountDetailRepository detailRepository, ProductStockRepository stockRepository,
            StoreRepository storeRepository, InventoryMovementRepository movementRepository,
            AuditLogService auditLogService) {
        this.countRepository = countRepository;
        this.detailRepository = detailRepository;
        this.stockRepository = stockRepository;
        this.storeRepository = storeRepository;
        this.movementRepository = movementRepository;
        this.auditLogService = auditLogService;
    }

    @Override
    public InventoryCountResponse crear(CreateInventoryCountRequest request, User usuario) {
        Store store = resolverSucursal(request.getSucursalId(), usuario);
        if (countRepository.existsBySucursalIdAndStatus(store.getId(), InventoryCountStatus.IN_PROGRESS))
            throw new IllegalStateException("Ya existe un inventario físico en curso para esta sucursal");
        InventoryCount count = InventoryCount.builder().description(request.getDescripcion().trim())
                .createdBy(usuario).sucursal(store).status(InventoryCountStatus.DRAFT).build();
        InventoryCount saved = countRepository.save(count);
        auditLogService.record(
                "INVENTORY", "INVENTORY_COUNT", saved.getId().toString(), "CREATED",
                usuario, store, null,
                Map.of("status", saved.getStatus().name(), "description", saved.getDescription()),
                "Inventario físico #" + saved.getId() + " creado", "WEB"
        );
        return response(saved);
    }

    @Override
    public InventoryCountResponse iniciar(Long id, User usuario) {
        InventoryCount count = entidad(id, usuario);
        estado(count, InventoryCountStatus.DRAFT);
        if (countRepository.existsBySucursalIdAndStatus(count.getSucursal().getId(), InventoryCountStatus.IN_PROGRESS))
            throw new IllegalStateException("Ya existe otro inventario físico en curso");
        List<ProductStock> stocks = stockRepository.findBySucursalId(count.getSucursal().getId());
        if (stocks.isEmpty()) throw new IllegalStateException("La sucursal no tiene inventario inicializado");
        for (ProductStock ps : stocks) {
            if (!Boolean.TRUE.equals(ps.getProducto().getActive())) continue;
            detailRepository.save(InventoryCountDetail.builder().inventoryCount(count)
                    .product(ps.getProducto()).expectedStock(ps.getStock() == null ? BigDecimal.ZERO : ps.getStock())
                    .adjustmentApplied(false).build());
        }
        count.setStatus(InventoryCountStatus.IN_PROGRESS);
        count.setStartedAt(LocalDateTime.now());
        InventoryCount saved = countRepository.save(count);
        auditLogService.record(
                "INVENTORY", "INVENTORY_COUNT", saved.getId().toString(), "STATUS_CHANGED",
                usuario, saved.getSucursal(),
                Map.of("status", InventoryCountStatus.DRAFT.name()),
                Map.of("status", InventoryCountStatus.IN_PROGRESS.name()),
                "Inventario físico #" + saved.getId() + " iniciado", "WEB"
        );
        return response(saved);
    }

    @Override
    public InventoryCountResponse registrarConteo(Long id, InventoryCountLineRequest request, User usuario) {
        InventoryCount count = entidad(id, usuario);
        estado(count, InventoryCountStatus.IN_PROGRESS);
        InventoryCountDetail d = detailRepository.findByInventoryCountIdAndProductId(id, request.getProductoId())
                .orElseThrow(() -> new IllegalArgumentException("Producto fuera del inventario"));
        if (Boolean.TRUE.equals(d.getAdjustmentApplied())) throw new IllegalStateException("Ajuste ya aplicado");
        BigDecimal previousCount = d.getCountedStock();
        d.setCountedStock(request.getCantidadContada());
        d.setDifference(request.getCantidadContada().subtract(d.getExpectedStock()));
        d.setNotes(request.getObservaciones());
        detailRepository.save(d);
        auditLogService.record(
                "INVENTORY", "INVENTORY_COUNT_LINE", d.getId().toString(), "COUNT_RECORDED",
                usuario, count.getSucursal(),
                Map.of("countedStock", previousCount != null ? previousCount : d.getExpectedStock()),
                Map.of("countedStock", d.getCountedStock(), "difference", d.getDifference(),
                        "productId", d.getProduct().getId(), "inventoryCountId", count.getId()),
                "Conteo físico registrado para " + d.getProduct().getSku(), "WEB"
        );
        return response(count);
    }

    @Override
    public InventoryCountResponse completar(Long id, User usuario) {
        InventoryCount count = entidad(id, usuario);
        estado(count, InventoryCountStatus.IN_PROGRESS);
        List<InventoryCountDetail> lines = detailRepository.findByInventoryCountId(id);
        if (lines.isEmpty() || lines.stream().anyMatch(d -> d.getCountedStock() == null))
            throw new IllegalStateException("Falta contar uno o más productos");
        for (InventoryCountDetail d : lines) {
            BigDecimal delta = d.getCountedStock().subtract(d.getExpectedStock());
            if (delta.compareTo(BigDecimal.ZERO) != 0) {
                ProductStock ps = stockRepository.findByProductoAndSucursalForUpdate(
                        d.getProduct().getId(), count.getSucursal().getId())
                        .orElseThrow(() -> new IllegalStateException("Stock no inicializado: " + d.getProduct().getSku()));
                BigDecimal before = ps.getStock() == null ? BigDecimal.ZERO : ps.getStock();
                BigDecimal after = before.add(delta);
                if (after.compareTo(BigDecimal.ZERO) < 0
                        || after.compareTo(new BigDecimal("9999999999.9999")) > 0)
                    throw new IllegalStateException("Ajuste inválido: " + d.getProduct().getSku());
                ps.setStock(after);
                stockRepository.save(ps);
                InventoryMovement savedMovement = movementRepository.save(InventoryMovement.builder().producto(d.getProduct())
                        .sucursal(count.getSucursal()).tipoMovimiento(MovementType.ADJUSTMENT)
                        .cantidad(delta.abs()).stockAnterior(before).stockNuevo(after)
                        .motivo("Inventario físico #" + count.getId() + (d.getNotes() == null ? "" : " - " + d.getNotes()))
                        .tipoReferencia("INVENTORY_COUNT").referenciaId(count.getId()).usuario(usuario).build());
                auditLogService.record(
                        "INVENTORY", "INVENTORY_MOVEMENT", savedMovement.getId().toString(),
                        "STOCK_MOVED", usuario, count.getSucursal(),
                        Map.of("stock", before),
                        Map.of("stock", after, "movementType", MovementType.ADJUSTMENT.name(),
                                "quantity", delta.abs(), "inventoryCountId", count.getId(),
                                "productId", d.getProduct().getId()),
                        "Ajuste de stock por inventario físico #" + count.getId()
                                + " para " + d.getProduct().getSku(),
                        "INVENTORY_COUNT"
                );
            }
            d.setDifference(delta);
            d.setAdjustmentApplied(true);
            detailRepository.save(d);
        }
        count.setStatus(InventoryCountStatus.COMPLETED);
        count.setCompletedAt(LocalDateTime.now());
        InventoryCount saved = countRepository.save(count);
        auditLogService.record(
                "INVENTORY", "INVENTORY_COUNT", saved.getId().toString(), "STATUS_CHANGED",
                usuario, saved.getSucursal(),
                Map.of("status", InventoryCountStatus.IN_PROGRESS.name()),
                Map.of("status", InventoryCountStatus.COMPLETED.name(),
                        "completedAt", saved.getCompletedAt().toString()),
                "Inventario físico #" + saved.getId() + " completado", "WEB"
        );
        return response(saved);
    }

    @Override
    public InventoryCountResponse cancelar(Long id, User usuario) {
        InventoryCount c = entidad(id, usuario);
        if (c.getStatus() == InventoryCountStatus.COMPLETED || c.getStatus() == InventoryCountStatus.CANCELED)
            throw new IllegalStateException("El inventario ya está cerrado");
        InventoryCountStatus previousStatus = c.getStatus();
        c.setStatus(InventoryCountStatus.CANCELED);
        InventoryCount saved = countRepository.save(c);
        auditLogService.record(
                "INVENTORY", "INVENTORY_COUNT", saved.getId().toString(), "STATUS_CHANGED",
                usuario, saved.getSucursal(),
                Map.of("status", previousStatus.name()),
                Map.of("status", InventoryCountStatus.CANCELED.name()),
                "Inventario físico #" + saved.getId() + " cancelado", "WEB"
        );
        return response(saved);
    }

    @Override @Transactional(readOnly = true)
    public InventoryCountResponse obtener(Long id, User usuario) { return response(entidad(id, usuario)); }

    @Override @Transactional(readOnly = true)
    public List<InventoryCountResponse> listar(User usuario) {
        List<InventoryCount> all = usuario.getRole() == Role.ADMIN
                ? countRepository.findAllByOrderByCreatedAtDesc()
                : countRepository.findBySucursalIdOrderByCreatedAtDesc(requireStore(usuario).getId());
        return all.stream().map(this::response).toList();
    }

    private InventoryCount entidad(Long id, User u) {
        InventoryCount c = countRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Inventario no encontrado: " + id));
        if (u.getRole() != Role.ADMIN && (u.getSucursal() == null || !u.getSucursal().getId().equals(c.getSucursal().getId())))
            throw new IllegalArgumentException("Sin permiso para este inventario");
        return c;
    }
    private Store resolverSucursal(Long id, User u) {
        if (u.getRole() != Role.ADMIN) return requireStore(u);
        if (id == null) id = storeRepository.findByCodigo("SAN_FRANCISCO")
                .orElseThrow(() -> new IllegalStateException("No existe SAN_FRANCISCO")).getId();
        Long storeId = id;
        return storeRepository.findById(storeId).filter(s -> Boolean.TRUE.equals(s.getActiva()))
                .orElseThrow(() -> new IllegalArgumentException("Sucursal inexistente o inactiva"));
    }
    private Store requireStore(User u) {
        if (u.getSucursal() == null) throw new IllegalStateException("Usuario sin sucursal asignada");
        return u.getSucursal();
    }
    private void estado(InventoryCount c, InventoryCountStatus s) {
        if (c.getStatus() != s) throw new IllegalStateException("Estado requerido " + s + "; actual " + c.getStatus());
    }
    private InventoryCountResponse response(InventoryCount c) {
        List<InventoryCountDetailResponse> details = detailRepository.findByInventoryCountId(c.getId()).stream()
                .map(d -> InventoryCountDetailResponse.builder().id(d.getId()).productoId(d.getProduct().getId())
                        .sku(d.getProduct().getSku()).nombreProducto(d.getProduct().getName())
                        .stockEsperado(d.getExpectedStock()).stockContado(d.getCountedStock())
                        .diferencia(d.getDifference()).observaciones(d.getNotes())
                        .ajusteAplicado(d.getAdjustmentApplied()).build()).toList();
        return InventoryCountResponse.builder().id(c.getId()).descripcion(c.getDescription()).estado(c.getStatus())
                .sucursalId(c.getSucursal().getId()).nombreSucursal(c.getSucursal().getNombre())
                .fechaCreacion(c.getCreatedAt()).iniciadoEn(c.getStartedAt()).completadoEn(c.getCompletedAt())
                .detalles(details).build();
    }
}
