package com.ferreteria.fc.web.controller;

import com.ferreteria.fc.domain.dto.mapper.ProductMapper;
import com.ferreteria.fc.domain.dto.request.CreateProductRequest;
import com.ferreteria.fc.domain.dto.request.PatchProductRequest;
import com.ferreteria.fc.domain.dto.request.UpdateProductRequest;
import com.ferreteria.fc.domain.dto.response.ProductResponse;
import com.ferreteria.fc.domain.service.ProductService;
import org.springframework.http.MediaType;
import org.springframework.web.multipart.MultipartFile;
import jakarta.validation.Valid;
import com.ferreteria.fc.domain.service.UserService;
import com.ferreteria.fc.persistence.entity.User;
import com.ferreteria.fc.domain.enums.Role;
import org.springframework.security.core.Authentication;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/products")
@CrossOrigin(origins = "http://localhost:4200")
public class ProductController {

    private final ProductService productService;
    private final ProductMapper productMapper;
    private final UserService userService;

    public ProductController(
            ProductService productService, ProductMapper productMapper, UserService userService
    ) {
        this.productService = productService;
        this.productMapper = productMapper;
        this.userService = userService;
    }

    @PostMapping
    public ResponseEntity<ProductResponse> createProduct(
            @RequestBody @Valid CreateProductRequest request
    ) {

        return ResponseEntity.ok(
                productService.createProduct(request)
        );
    }

    @GetMapping
    public ResponseEntity<List<ProductResponse>> getAll(
            @RequestParam(required = false) Long sucursalId,
            Authentication authentication
    ) {
        User usuario = obtenerUsuario(authentication);

        Long sucursalAutorizada =
                resolverSucursalId(usuario, sucursalId);

        return ResponseEntity.ok(
                productService.getAll(sucursalAutorizada)
        );
    }

    @GetMapping("/{id}")
    public ResponseEntity<ProductResponse> getById(
            @PathVariable Long id,
            @RequestParam(required = false) Long sucursalId,
            Authentication authentication
    ) {
        User usuario = obtenerUsuario(authentication);

        Long sucursalAutorizada =
                resolverSucursalId(usuario, sucursalId);

        return ResponseEntity.ok(
                productService.getById(id, sucursalAutorizada)
        );
    }

    @PutMapping("/{id}")
    public ResponseEntity<ProductResponse> update(
            @PathVariable Long id,
            @RequestBody @Valid UpdateProductRequest request
    ) {

        return ResponseEntity.ok(
                productService.updateProduct(id, request)
        );
    }

    @PatchMapping("/{id}")
    public ResponseEntity<ProductResponse> patch(
            @PathVariable Long id,
            @RequestBody PatchProductRequest request
    ) {

        return ResponseEntity.ok(
                productService.patchProduct(id, request)
        );
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(
            @PathVariable Long id
    ) {

        productService.delete(id);

        return ResponseEntity.noContent().build();
    }

    @GetMapping("/search")
    public ResponseEntity<List<ProductResponse>> search(
            @RequestParam String query,
            @RequestParam(required = false) Long sucursalId,
            Authentication authentication
    ) {

        User usuario = obtenerUsuario(authentication);

        Long sucursalAutorizada =
                resolverSucursalId(usuario, sucursalId);

        return ResponseEntity.ok(
                productService.search(
                        query,
                        sucursalAutorizada
                )
        );
    }

    @PutMapping(
            value = "/{id}/image",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    public ResponseEntity<ProductResponse> uploadImage(
            @PathVariable Long id,
            @RequestParam("file") MultipartFile file
    ) {

        return ResponseEntity.ok(
                productService.updateProductImage(id, file)
        );

    }


    @DeleteMapping("/{id}/image")
    public ResponseEntity<ProductResponse> deleteImage(
            @PathVariable Long id
    ) {

        return ResponseEntity.ok(
                productService.deleteProductImage(id)
        );

    }
    private Long resolverSucursalId(
            User usuario,
            Long sucursalId
    ) {
        if (usuario.getRole() == Role.ADMIN) {
            if (sucursalId == null) {
                throw new IllegalArgumentException(
                        "Debe seleccionar una sucursal"
                );
            }

            return sucursalId;
        }

        if (usuario.getSucursal() == null) {
            throw new IllegalArgumentException(
                    "El usuario no tiene una sucursal asignada"
            );
        }

        // El vendedor nunca puede elegir otra sucursal.
        return usuario.getSucursal().getId();
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