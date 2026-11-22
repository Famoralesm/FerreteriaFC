
package com.ferreteria.fc.web.controller;

import com.ferreteria.fc.domain.dto.response.ProductStockResponse;
import com.ferreteria.fc.domain.dto.request.InitialStockRequest;
import com.ferreteria.fc.domain.dto.request.ProductStockMinimumRequest;
import com.ferreteria.fc.domain.enums.InventoryCountStatus;
import com.ferreteria.fc.domain.enums.MovementType;
import com.ferreteria.fc.domain.repository.InventoryCountRepository;
import com.ferreteria.fc.domain.repository.InventoryMovementRepository;
import com.ferreteria.fc.domain.repository.ProductRepository;
import com.ferreteria.fc.domain.enums.Role;
import com.ferreteria.fc.domain.repository.ProductStockRepository;
import com.ferreteria.fc.domain.repository.StoreRepository;
import com.ferreteria.fc.domain.service.UserService;
import com.ferreteria.fc.persistence.entity.ProductStock;
import com.ferreteria.fc.persistence.entity.InventoryMovement;
import com.ferreteria.fc.persistence.entity.Store;
import com.ferreteria.fc.persistence.entity.User;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.transaction.annotation.Transactional;
import jakarta.validation.Valid;

import java.util.List;
import java.math.BigDecimal;

@RestController
@RequestMapping("/api/product-stocks")
@CrossOrigin(origins = "http://localhost:4200")
public class ProductStockController {

    private final ProductStockRepository productStockRepository;
    private final StoreRepository storeRepository;
    private final UserService userService;
    private final ProductRepository productRepository;
    private final InventoryMovementRepository movementRepository;
    private final InventoryCountRepository inventoryCountRepository;

    public ProductStockController(
            ProductStockRepository productStockRepository,
            StoreRepository storeRepository,
            UserService userService,
            ProductRepository productRepository,
            InventoryMovementRepository movementRepository,
            InventoryCountRepository inventoryCountRepository
    ) {
        this.productStockRepository = productStockRepository;
        this.storeRepository = storeRepository;
        this.userService = userService;
        this.productRepository = productRepository;
        this.movementRepository = movementRepository;
        this.inventoryCountRepository = inventoryCountRepository;
    }

    @GetMapping
    public ResponseEntity<List<ProductStockResponse>> getByStore(
            @RequestParam(required = false) Long sucursalId,
            Authentication authentication
    ) {
        User usuario = obtenerUsuario(authentication);
        Long sucursalAutorizada = resolverSucursalId(usuario, sucursalId);

        validarSucursalActiva(sucursalAutorizada);

        List<ProductStockResponse> response =
                productStockRepository.findActiveProductsBySucursalId(sucursalAutorizada)
                        .stream()
                        .map(this::toResponse)
                        .toList();

        return ResponseEntity.ok(response);
    }

    @GetMapping("/low-stock")
    public ResponseEntity<List<ProductStockResponse>> getLowStockByStore(
            @RequestParam(required = false) Long sucursalId,
            Authentication authentication
    ) {
        User usuario = obtenerUsuario(authentication);
        Long sucursalAutorizada = resolverSucursalId(usuario, sucursalId);

        validarSucursalActiva(sucursalAutorizada);

        List<ProductStockResponse> response =
                productStockRepository.findLowStockBySucursalId(sucursalAutorizada)
                        .stream()
                        .map(this::toResponse)
                        .toList();

        return ResponseEntity.ok(response);
    }

    @GetMapping("/product/{productoId}")
    public ResponseEntity<List<ProductStockResponse>> getByProduct(
            @PathVariable Long productoId,
            @RequestParam(required = false) Long sucursalId,
            Authentication authentication
    ) {
        User usuario = obtenerUsuario(authentication);
        Long sucursalAutorizada = resolverSucursalId(usuario, sucursalId);

        validarSucursalActiva(sucursalAutorizada);

        List<ProductStockResponse> response =
                productStockRepository.findByProductoId(productoId)
                        .stream()
                        .filter(ps -> ps.getSucursal().getId()
                                .equals(sucursalAutorizada))
                        .map(this::toResponse)
                        .toList();

        return ResponseEntity.ok(response);
    }


    /**
     * Registra el conteo físico de apertura una sola vez por producto y sucursal.
     * Reemplaza cualquier saldo de prueba sin historial y deja trazabilidad del saldo anterior.
     */
    @PostMapping("/initialization")
    @Transactional
    public ResponseEntity<ProductStockResponse> initializeStock(
            @Valid @RequestBody InitialStockRequest request,
            Authentication authentication
    ) {
        User usuario = obtenerUsuario(authentication);
        Long sucursalId = resolverSucursalId(usuario, request.getSucursalId());

        validarSucursalActiva(sucursalId);
        validarSinInventarioEnCurso(sucursalId);

        var producto = productRepository.findById(request.getProductoId())
                .orElseThrow(() -> new IllegalArgumentException(
                        "Producto no encontrado: " + request.getProductoId()
                ));

        if (!Boolean.TRUE.equals(producto.getActive())) {
            throw new IllegalArgumentException(
                    "No se puede inicializar el stock de un producto inactivo"
            );
        }

        ProductStock stock = productStockRepository
                .findByProductoAndSucursalForUpdate(producto.getId(), sucursalId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "No existe registro de stock para el producto y sucursal seleccionados"
                ));

        if (movementRepository.existsByProductoIdAndSucursalId(producto.getId(), sucursalId)) {
            throw new IllegalStateException(
                    "El inventario inicial ya fue registrado para este producto y sucursal. "
                            + "Para corregir existencias, utiliza un ajuste de inventario."
            );
        }

        BigDecimal anterior = stock.getStock() != null
                ? stock.getStock() : BigDecimal.ZERO;
        BigDecimal nuevo = request.getStock();

        stock.setStock(nuevo);
        stock.setStockMinimo(request.getStockMinimo());
        productStockRepository.saveAndFlush(stock);

        String motivoInicial = request.getMotivo() == null || request.getMotivo().isBlank()
                ? "Inventario inicial"
                : request.getMotivo().trim();
        if (anterior.compareTo(BigDecimal.ZERO) > 0) {
            motivoInicial += " (saldo provisional anterior reemplazado: "
                    + anterior.toPlainString() + ")";
        }

        InventoryMovement movimiento = InventoryMovement.builder()
                .producto(producto)
                .sucursal(stock.getSucursal())
                .tipoMovimiento(MovementType.INITIAL)
                .cantidad(nuevo)
                .stockAnterior(BigDecimal.ZERO)
                .stockNuevo(nuevo)
                .motivo(motivoInicial)
                .tipoReferencia("INITIAL_STOCK")
                .referenciaId(stock.getId())
                .usuario(usuario)
                .build();

        movementRepository.save(movimiento);

        return ResponseEntity.ok(toResponse(stock));
    }

    /**
     * Permite modificar el umbral de reposición sin alterar las existencias.
     */
    @PutMapping("/{productoId}/minimum")
    @Transactional
    public ResponseEntity<ProductStockResponse> updateMinimumStock(
            @PathVariable Long productoId,
            @Valid @RequestBody ProductStockMinimumRequest request,
            Authentication authentication
    ) {
        User usuario = obtenerUsuario(authentication);
        Long sucursalId = resolverSucursalId(usuario, request.getSucursalId());

        validarSucursalActiva(sucursalId);
        validarSinInventarioEnCurso(sucursalId);

        ProductStock stock = productStockRepository
                .findByProductoAndSucursalForUpdate(productoId, sucursalId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "No existe registro de stock para el producto y sucursal seleccionados"
                ));

        stock.setStockMinimo(request.getStockMinimo());
        return ResponseEntity.ok(toResponse(productStockRepository.save(stock)));
    }

    private void validarSinInventarioEnCurso(Long sucursalId) {
        if (inventoryCountRepository.existsBySucursalIdAndStatus(
                sucursalId, InventoryCountStatus.IN_PROGRESS)) {
            throw new IllegalStateException(
                    "No se pueden modificar existencias mientras exista un inventario físico en curso"
            );
        }
    }

    private ProductStockResponse toResponse(ProductStock productoStock) {
        var producto = productoStock.getProducto();
        var sucursal = productoStock.getSucursal();
        BigDecimal stock = productoStock.getStock() != null
                ? productoStock.getStock() : BigDecimal.ZERO;
        BigDecimal minimo = productoStock.getStockMinimo() != null
                ? productoStock.getStockMinimo() : BigDecimal.ZERO;

        return ProductStockResponse.builder()
                .id(productoStock.getId())
                .productoId(producto.getId())
                .sku(producto.getSku())
                .nombreProducto(producto.getName())
                .categoria(producto.getCategory())
                .marca(producto.getBrand())
                .sucursalId(sucursal.getId())
                .nombreSucursal(sucursal.getNombre())
                .stock(stock)
                .stockMinimo(minimo)
                .bajoStock(stock.compareTo(minimo) <= 0)
                .fechaActualizacion(productoStock.getFechaActualizacion())
                .inventarioInicialRegistrado(movementRepository.existsByProductoIdAndSucursalId(
                        producto.getId(), sucursal.getId()))
                .build();
    }

    private Long resolverSucursalId(User usuario, Long sucursalId) {
        if (usuario.getRole() == Role.ADMIN) {
            if (sucursalId == null) {
                throw new IllegalArgumentException(
                        "Debe indicar la sucursal que desea consultar"
                );
            }

            return sucursalId;
        }

        if (usuario.getSucursal() == null) {
            throw new IllegalArgumentException(
                    "El usuario no tiene una sucursal asignada"
            );
        }

        // Ignoramos cualquier sucursal enviada por un usuario no administrador.
        return usuario.getSucursal().getId();
    }

    private void validarSucursalActiva(Long sucursalId) {
        Store sucursal = storeRepository.findById(sucursalId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Sucursal no encontrada: " + sucursalId
                ));

        if (!Boolean.TRUE.equals(sucursal.getActiva())) {
            throw new IllegalArgumentException(
                    "La sucursal seleccionada está inactiva"
            );
        }
    }

    private User obtenerUsuario(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new IllegalStateException(
                    "No existe un usuario autenticado"
            );
        }

        return userService.getByEmail(authentication.getName());
    }
}
