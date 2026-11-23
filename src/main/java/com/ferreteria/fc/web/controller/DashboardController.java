package com.ferreteria.fc.web.controller;

import com.ferreteria.fc.domain.dto.response.DashboardOrderStatusResponse;
import com.ferreteria.fc.domain.dto.response.DashboardProductStatusResponse;
import com.ferreteria.fc.domain.dto.response.DashboardResponse;
import com.ferreteria.fc.domain.enums.Role;
import com.ferreteria.fc.domain.repository.StoreRepository;
import com.ferreteria.fc.domain.service.DashboardService;
import com.ferreteria.fc.domain.service.UserService;
import com.ferreteria.fc.persistence.entity.Store;
import com.ferreteria.fc.persistence.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final DashboardService dashboardService;
    private final UserService userService;
    private final StoreRepository storeRepository;

    @GetMapping
    public DashboardResponse getDashboard() {
        return dashboardService.getDashboard();
    }

    @GetMapping("/orders-by-status")
    public ResponseEntity<DashboardOrderStatusResponse> getOrdersByStatus(
            @RequestParam Long storeId,
            @RequestParam LocalDateTime from,
            @RequestParam LocalDateTime to,
            Authentication authentication
    ) {
        Long authorizedStoreId = resolveStoreId(authentication, storeId);
        if (from == null || to == null || !from.isBefore(to)) {
            throw new IllegalArgumentException("El período de consulta no es válido");
        }

        return ResponseEntity.ok(
                dashboardService.getOrdersByStatusAndPeriod(authorizedStoreId, from, to)
        );
    }

    @GetMapping("/products-status")
    public ResponseEntity<DashboardProductStatusResponse> getProductStatus(
            @RequestParam Long storeId,
            Authentication authentication
    ) {
        Long authorizedStoreId = resolveStoreId(authentication, storeId);
        return ResponseEntity.ok(
                dashboardService.getProductStatusByStore(authorizedStoreId)
        );
    }

    private Long resolveStoreId(Authentication authentication, Long requestedStoreId) {
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new IllegalStateException("No existe un usuario autenticado");
        }

        User user = userService.getByEmail(authentication.getName());
        Long storeId;

        if (user.getRole() == Role.ADMIN) {
            storeId = requestedStoreId;
        } else {
            if (user.getSucursal() == null) {
                throw new IllegalArgumentException("El usuario no tiene una sucursal asignada");
            }
            storeId = user.getSucursal().getId();
        }

        Store store = storeRepository.findById(storeId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Sucursal no encontrada: " + storeId
                ));

        if (!Boolean.TRUE.equals(store.getActiva())) {
            throw new IllegalArgumentException("La sucursal seleccionada está inactiva");
        }

        return storeId;
    }
}
