
package com.ferreteria.fc.web.controller;

import com.ferreteria.fc.domain.dto.response.ProductStockResponse;
import com.ferreteria.fc.domain.enums.Role;
import com.ferreteria.fc.domain.repository.ProductStockRepository;
import com.ferreteria.fc.domain.repository.StoreRepository;
import com.ferreteria.fc.domain.service.UserService;
import com.ferreteria.fc.persistence.entity.ProductStock;
import com.ferreteria.fc.persistence.entity.Store;
import com.ferreteria.fc.persistence.entity.User;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.math.BigDecimal;

@RestController
@RequestMapping("/api/product-stocks")
@CrossOrigin(origins = "http://localhost:4200")
public class ProductStockController {

    private final ProductStockRepository productStockRepository;
    private final StoreRepository storeRepository;
    private final UserService userService;

    public ProductStockController(
            ProductStockRepository productStockRepository,
            StoreRepository storeRepository,
            UserService userService
    ) {
        this.productStockRepository = productStockRepository;
        this.storeRepository = storeRepository;
        this.userService = userService;
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
                productStockRepository.findBySucursalId(sucursalAutorizada)
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

    private ProductStockResponse toResponse(ProductStock productoStock) {
        var producto = productoStock.getProducto();
        var sucursal = productoStock.getSucursal();

        BigDecimal stock = productoStock.getStock() != null
                ? productoStock.getStock()
                : BigDecimal.ZERO;

        BigDecimal stockMinimo = productoStock.getStockMinimo() != null
                ? productoStock.getStockMinimo()
                : BigDecimal.ZERO;

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
                .stockMinimo(stockMinimo)
                .bajoStock(stock.compareTo(stockMinimo) <= 0)
                .fechaActualizacion(productoStock.getFechaActualizacion())
                .build();
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
