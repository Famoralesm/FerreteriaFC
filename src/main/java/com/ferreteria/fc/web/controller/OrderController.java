package com.ferreteria.fc.web.controller;

import com.ferreteria.fc.domain.dto.request.CreateOrderRequest;
import com.ferreteria.fc.domain.dto.request.UpdateOrderRequest;
import com.ferreteria.fc.domain.dto.request.UpdatePatchOrderRequest;
import com.ferreteria.fc.domain.dto.response.OrderResponse;
import com.ferreteria.fc.domain.dto.response.OrderSummaryResponse;
import com.ferreteria.fc.domain.service.OrderService;
import com.ferreteria.fc.domain.service.UserService;
import com.ferreteria.fc.persistence.entity.User;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Objects;

@RestController
@RequestMapping("/api/orders")
@CrossOrigin(origins = "http://localhost:4200")
public class OrderController {

    private final OrderService orderService;
    private final UserService userService;

    public OrderController(OrderService orderService, UserService userService) {
        this.orderService = orderService;
        this.userService = userService;
    }

    @PostMapping
    public ResponseEntity<OrderResponse> createOrder(@RequestBody @Valid CreateOrderRequest request) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()
                || "anonymousUser".equals(authentication.getPrincipal())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Debes iniciar sesión para crear un pedido");
        }

        User user = userService.getOptionalByEmail(authentication.getName())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.UNAUTHORIZED, "No se pudo identificar al usuario autenticado"));

        return ResponseEntity.ok(orderService.createOrder(request, user));
    }

    @GetMapping
    public ResponseEntity<List<OrderResponse>> getAll() {
        return ResponseEntity.ok(orderService.getAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<OrderResponse> get(@PathVariable Long id) {
        return ResponseEntity.ok(orderService.getById(id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<OrderResponse> update(@PathVariable Long id, @RequestBody @Valid UpdateOrderRequest request) {
        OrderResponse current = orderService.getById(id);
        if (current.getStatus() == com.ferreteria.fc.domain.enums.OrderStatus.IN_ROUTE
                && request.getItems() != null && !sameItems(current, request)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Un pedido en ruta no permite agregar, eliminar ni modificar productos.");
        }
        return ResponseEntity.ok(orderService.updateOrder(id, request));
    }

    @PatchMapping("/{id}")
    public ResponseEntity<OrderResponse> patch(@PathVariable Long id, @RequestBody UpdatePatchOrderRequest request) {
        return ResponseEntity.ok(orderService.patchOrder(id, request));
    }

    private boolean sameItems(OrderResponse current, UpdateOrderRequest request) {
        var currentItems = current.getItems() == null
                ? List.<com.ferreteria.fc.domain.dto.response.OrderItemResponse>of()
                : current.getItems();
        var requestedItems = request.getItems();
        if (currentItems.size() != requestedItems.size()) return false;
        for (int i = 0; i < currentItems.size(); i++) {
            var before = currentItems.get(i);
            var after = requestedItems.get(i);
            if (!Objects.equals(before.getProductId(), after.getProductId())
                    || !Objects.equals(before.getUnitId(), after.getUnitId())
                    || before.getQuantity().compareTo(after.getQuantity()) != 0
                    || (before.getUnitPrice() != null && after.getUnitPrice() != null
                        && before.getUnitPrice().compareTo(after.getUnitPrice()) != 0)) {
                return false;
            }
        }
        return true;
    }

    @GetMapping("/latest")
    public List<OrderSummaryResponse> latestOrders(@RequestParam Long storeId) {
        return orderService.findLatestOrders(storeId);
    }

    @GetMapping("/all")
    public List<OrderSummaryResponse> findAll(@RequestParam Long storeId) {
        return orderService.findAllSummary(storeId);
    }
}