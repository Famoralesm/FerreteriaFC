package com.ferreteria.fc.web.controller;

import com.ferreteria.fc.domain.dto.request.CreateOrderRequest;
import com.ferreteria.fc.domain.dto.request.UpdateOrderRequest;
import com.ferreteria.fc.domain.dto.request.UpdatePatchOrderRequest;
import com.ferreteria.fc.domain.dto.response.OrderResponse;
import com.ferreteria.fc.domain.service.OrderService;
import com.ferreteria.fc.domain.service.UserService;
import com.ferreteria.fc.persistence.entity.User;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

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
        System.out.println("ENTRO AL POST");
        Authentication authentication =
                SecurityContextHolder.getContext().getAuthentication();
        System.out.println("User name "+authentication.getName());
        User user = userService.getByEmail(authentication.getName());

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
}