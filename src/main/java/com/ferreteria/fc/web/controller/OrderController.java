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
import java.util.Optional;

@RestController
@RequestMapping("/api/orders")
@CrossOrigin(origins = "http://localhost:4200")
public class OrderController {

    private final OrderService orderService;
    private final UserService userService;

    public OrderController(OrderService orderService,
                           UserService userService) {
        this.orderService = orderService;
        this.userService = userService;
    }

    @PostMapping
    public ResponseEntity<OrderResponse> createOrder(
            @RequestBody CreateOrderRequest request) {

        Authentication authentication =
                SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null ||
                !authentication.isAuthenticated() ||
                "anonymousUser".equals(authentication.getPrincipal())) {

            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "Debes iniciar sesión para crear un pedido"
            );
        }

        User user = userService.getOptionalByEmail(authentication.getName())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.UNAUTHORIZED,
                        "No se pudo identificar al usuario autenticado"
                ));

        OrderResponse response = orderService.createOrder(request, user);

        return ResponseEntity.ok(response);
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
    public ResponseEntity<OrderResponse> update(
            @PathVariable Long id,
            @RequestBody @Valid UpdateOrderRequest request) {

        return ResponseEntity.ok(orderService.updateOrder(id, request));
    }

    @PatchMapping("/{id}")
    public ResponseEntity<OrderResponse> patch(
            @PathVariable Long id,
            @RequestBody UpdatePatchOrderRequest request) {

        return ResponseEntity.ok(orderService.patchOrder(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        orderService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/latest")
    public List<OrderSummaryResponse> latestOrders(){

        return orderService.findLatestOrders();

    }
    @GetMapping("/all")
    public List<OrderSummaryResponse> findAll() {

        return orderService.findAllSummary();

    }
}