package com.ferreteria.fc.web.controller;

import com.ferreteria.fc.domain.service.PurchaseService;
import com.ferreteria.fc.domain.service.UserService;
import com.ferreteria.fc.dto.purchase.PurchaseRequest;
import com.ferreteria.fc.dto.purchase.PurchaseResponse;
import com.ferreteria.fc.persistence.entity.User;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/purchases")
@CrossOrigin(origins = "http://localhost:4200")
public class PurchaseController {

    private final PurchaseService purchaseService;
    private final UserService userService;

    public PurchaseController(
            PurchaseService purchaseService,
            UserService userService
    ) {
        this.purchaseService = purchaseService;
        this.userService = userService;
    }

    @GetMapping
    public ResponseEntity<java.util.List<PurchaseResponse>> listar(Authentication authentication) {
        User usuario = obtenerUsuario(authentication);
        return ResponseEntity.ok(purchaseService.getAll(usuario));
    }

    @GetMapping("/{purchaseId}")
    public ResponseEntity<PurchaseResponse> obtener(
            @PathVariable Long purchaseId,
            Authentication authentication
    ) {
        User usuario = obtenerUsuario(authentication);
        return ResponseEntity.ok(purchaseService.getById(purchaseId, usuario));
    }

    @PostMapping
    public ResponseEntity<PurchaseResponse> register(
            @Valid @RequestBody PurchaseRequest request,
            Authentication authentication
    ) {
        User usuario = obtenerUsuario(authentication);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(purchaseService.register(request, usuario));
    }

    @PostMapping("/{purchaseId}/receive")
    public ResponseEntity<PurchaseResponse> receive(
            @PathVariable Long purchaseId,
            Authentication authentication
    ) {
        User usuario = obtenerUsuario(authentication);

        return ResponseEntity.ok(
                purchaseService.receive(purchaseId, usuario)
        );
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
