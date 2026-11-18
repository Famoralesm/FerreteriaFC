
package com.ferreteria.fc.web.controller;

import com.ferreteria.fc.domain.dto.response.StoreResponse;
import com.ferreteria.fc.domain.enums.Role;
import com.ferreteria.fc.domain.repository.StoreRepository;
import com.ferreteria.fc.domain.service.UserService;
import com.ferreteria.fc.persistence.entity.Store;
import com.ferreteria.fc.persistence.entity.User;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/stores")
@CrossOrigin(origins = "http://localhost:4200")
public class StoreController {

    private final StoreRepository storeRepository;
    private final UserService userService;

    public StoreController(
            StoreRepository storeRepository,
            UserService userService
    ) {
        this.storeRepository = storeRepository;
        this.userService = userService;
    }

    @GetMapping("/active")
    public ResponseEntity<List<StoreResponse>> getActiveStores(
            Authentication authentication
    ) {
        User usuario = obtenerUsuario(authentication);

        List<Store> sucursales;

        if (usuario.getRole() == Role.ADMIN) {
            sucursales = storeRepository.findByActivaTrueOrderByNombreAsc();
        } else {
            if (usuario.getSucursal() == null
                    || !Boolean.TRUE.equals(usuario.getSucursal().getActiva())) {
                return ResponseEntity.ok(List.of());
            }

            sucursales = List.of(usuario.getSucursal());
        }

        return ResponseEntity.ok(
                sucursales.stream()
                        .map(this::toResponse)
                        .toList()
        );
    }

    private StoreResponse toResponse(Store sucursal) {
        return StoreResponse.builder()
                .id(sucursal.getId())
                .nombre(sucursal.getNombre())
                .codigo(sucursal.getCodigo())
                .direccion(sucursal.getDireccion())
                .activa(sucursal.getActiva())
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
