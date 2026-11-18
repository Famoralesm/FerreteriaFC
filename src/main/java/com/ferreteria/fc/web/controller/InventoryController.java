
package com.ferreteria.fc.web.controller;

import com.ferreteria.fc.domain.dto.request.InventoryMovementRequest;
import com.ferreteria.fc.domain.dto.response.InventoryMovementResponse;
import com.ferreteria.fc.domain.service.InventoryService;
import com.ferreteria.fc.domain.service.UserService;
import com.ferreteria.fc.persistence.entity.User;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/inventory")
@CrossOrigin(origins = "http://localhost:4200")
public class InventoryController {

    private final InventoryService inventoryService;
    private final UserService userService;

    public InventoryController(
            InventoryService inventoryService,
            UserService userService
    ) {
        this.inventoryService = inventoryService;
        this.userService = userService;
    }

    @PostMapping("/entries")
    public ResponseEntity<InventoryMovementResponse> registerEntry(
            @Valid @RequestBody InventoryMovementRequest request,
            Authentication authentication
    ) {
        User usuario = obtenerUsuario(authentication);

        return ResponseEntity.ok(
                inventoryService.registerEntry(request, usuario)
        );
    }

    @PostMapping("/exits")
    public ResponseEntity<InventoryMovementResponse> registerExit(
            @Valid @RequestBody InventoryMovementRequest request,
            Authentication authentication
    ) {
        User usuario = obtenerUsuario(authentication);

        return ResponseEntity.ok(
                inventoryService.registerExit(request, usuario)
        );
    }

    @PostMapping("/adjustments")
    public ResponseEntity<InventoryMovementResponse> registerAdjustment(
            @Valid @RequestBody InventoryMovementRequest request,
            Authentication authentication
    ) {
        User usuario = obtenerUsuario(authentication);

        return ResponseEntity.ok(
                inventoryService.registerAdjustment(request, usuario)
        );
    }

    @GetMapping("/movements")
    public ResponseEntity<List<InventoryMovementResponse>> getMovements(
            @RequestParam Long productoId,
            @RequestParam(required = false) Long sucursalId,
            Authentication authentication
    ) {
        User usuario = obtenerUsuario(authentication);

        return ResponseEntity.ok(
                inventoryService.getMovements(
                        productoId,
                        sucursalId,
                        usuario
                )
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
