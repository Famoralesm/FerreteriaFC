package com.ferreteria.fc.web.controller;

import com.ferreteria.fc.domain.dto.request.*;
import com.ferreteria.fc.domain.dto.response.InventoryCountResponse;
import com.ferreteria.fc.domain.service.InventoryCountService;
import com.ferreteria.fc.domain.service.UserService;
import com.ferreteria.fc.persistence.entity.User;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/inventory-counts")
@CrossOrigin(origins = "http://localhost:4200")
public class InventoryCountController {
    private final InventoryCountService service;
    private final UserService userService;
    public InventoryCountController(InventoryCountService service, UserService userService) {
        this.service = service; this.userService = userService;
    }
    @PostMapping
    public ResponseEntity<InventoryCountResponse> crear(@Valid @RequestBody CreateInventoryCountRequest r, Authentication a) {
        return ResponseEntity.ok(service.crear(r, usuario(a)));
    }
    @PostMapping("/{id}/start")
    public ResponseEntity<InventoryCountResponse> iniciar(@PathVariable Long id, Authentication a) {
        return ResponseEntity.ok(service.iniciar(id, usuario(a)));
    }
    @PutMapping("/{id}/lines")
    public ResponseEntity<InventoryCountResponse> contar(@PathVariable Long id, @Valid @RequestBody InventoryCountLineRequest r, Authentication a) {
        return ResponseEntity.ok(service.registrarConteo(id, r, usuario(a)));
    }
    @PostMapping("/{id}/complete")
    public ResponseEntity<InventoryCountResponse> completar(@PathVariable Long id, Authentication a) {
        return ResponseEntity.ok(service.completar(id, usuario(a)));
    }
    @PostMapping("/{id}/cancel")
    public ResponseEntity<InventoryCountResponse> cancelar(@PathVariable Long id, Authentication a) {
        return ResponseEntity.ok(service.cancelar(id, usuario(a)));
    }
    @GetMapping("/{id}")
    public ResponseEntity<InventoryCountResponse> obtener(@PathVariable Long id, Authentication a) {
        return ResponseEntity.ok(service.obtener(id, usuario(a)));
    }
    @GetMapping
    public ResponseEntity<List<InventoryCountResponse>> listar(Authentication a) {
        return ResponseEntity.ok(service.listar(usuario(a)));
    }
    private User usuario(Authentication a) {
        if (a == null || !a.isAuthenticated()) throw new IllegalStateException("No existe usuario autenticado");
        return userService.getByEmail(a.getName());
    }
}
