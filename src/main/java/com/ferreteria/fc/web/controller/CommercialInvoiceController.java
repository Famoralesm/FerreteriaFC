package com.ferreteria.fc.web.controller;

import com.ferreteria.fc.domain.dto.request.CreateCommercialInvoiceRequest;
import com.ferreteria.fc.domain.dto.response.CommercialInvoiceResponse;
import com.ferreteria.fc.domain.service.CommercialInvoiceService;
import com.ferreteria.fc.domain.service.UserService;
import com.ferreteria.fc.persistence.entity.User;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;
import java.util.List;

@RestController
@PreAuthorize("hasAnyRole('ADMIN', 'SELLER')")
@RequestMapping("/api/invoices")
public class CommercialInvoiceController {
    private final CommercialInvoiceService invoiceService;
    private final UserService userService;

    public CommercialInvoiceController(CommercialInvoiceService invoiceService, UserService userService) {
        this.invoiceService = invoiceService;
        this.userService = userService;
    }

    @GetMapping
    public ResponseEntity<List<CommercialInvoiceResponse>> findAll(@RequestParam(required = false) Long storeId) {
        return ResponseEntity.ok(invoiceService.findAll(storeId, authenticatedUser()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<CommercialInvoiceResponse> findById(@PathVariable Long id) {
        return ResponseEntity.ok(invoiceService.findById(id, authenticatedUser()));
    }

    @PostMapping
    public ResponseEntity<CommercialInvoiceResponse> create(@Valid @RequestBody CreateCommercialInvoiceRequest request) {
        return ResponseEntity.ok(invoiceService.create(request, authenticatedUser()));
    }

    @PostMapping("/from-order/{orderId}")
    public ResponseEntity<CommercialInvoiceResponse> createFromOrder(@PathVariable Long orderId) {
        return ResponseEntity.ok(invoiceService.createFromOrder(orderId, authenticatedUser()));
    }

    @PostMapping("/{id}/cancel")
    public ResponseEntity<CommercialInvoiceResponse> cancel(@PathVariable Long id) {
        return ResponseEntity.ok(invoiceService.cancel(id, authenticatedUser()));
    }

    private User authenticatedUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()
                || "anonymousUser".equals(authentication.getPrincipal())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Debes iniciar sesión.");
        }
        return userService.getOptionalByEmail(authentication.getName())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "No se pudo identificar al usuario."));
    }
}
