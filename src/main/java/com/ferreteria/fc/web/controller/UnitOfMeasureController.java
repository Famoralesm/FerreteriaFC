package com.ferreteria.fc.web.controller;

import com.ferreteria.fc.domain.dto.request.UnitOfMeasureRequest;
import com.ferreteria.fc.domain.dto.response.UnitOfMeasureResponse;
import com.ferreteria.fc.domain.enums.Role;
import com.ferreteria.fc.domain.repository.UnitOfMeasureRepository;
import com.ferreteria.fc.domain.repository.ProductUnitRepository;
import com.ferreteria.fc.domain.repository.ProductRepository;
import com.ferreteria.fc.domain.service.UserService;
import com.ferreteria.fc.persistence.entity.UnitOfMeasure;
import com.ferreteria.fc.persistence.entity.User;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Locale;

@RestController
@RequestMapping("/api/units-of-measure")
@CrossOrigin(origins = "http://localhost:4200")
public class UnitOfMeasureController {
    private final UnitOfMeasureRepository repository;
    private final ProductUnitRepository productUnitRepository;
    private final ProductRepository productRepository;
    private final UserService userService;

    public UnitOfMeasureController(UnitOfMeasureRepository repository, ProductUnitRepository productUnitRepository, ProductRepository productRepository, UserService userService) {
        this.repository = repository;
        this.productUnitRepository = productUnitRepository;
        this.productRepository = productRepository;
        this.userService = userService;
    }

    @GetMapping
    public List<UnitOfMeasureResponse> list(Authentication auth) {
        validateAdmin(auth);
        return repository.findAllByOrderByNameAsc().stream().map(this::response).toList();
    }

    @PostMapping
    public ResponseEntity<UnitOfMeasureResponse> create(@Valid @RequestBody UnitOfMeasureRequest request, Authentication auth) {
        validateAdmin(auth);
        String code = normalize(request.getCode());
        if (repository.existsByCode(code)) throw new IllegalStateException("Ya existe una unidad con código " + code);
        UnitOfMeasure saved = repository.save(UnitOfMeasure.builder().code(code)
                .name(request.getName().trim()).symbol(request.getSymbol().trim()).active(true).build());
        return ResponseEntity.status(HttpStatus.CREATED).body(response(saved));
    }

    @PutMapping("/{id}")
    public UnitOfMeasureResponse update(@PathVariable Long id, @Valid @RequestBody UnitOfMeasureRequest request, Authentication auth) {
        validateAdmin(auth);
        UnitOfMeasure unit = repository.findById(id).orElseThrow(() -> new IllegalArgumentException("Unidad no encontrada: " + id));
        String code = normalize(request.getCode());
        repository.findByCode(code).filter(other -> !other.getId().equals(id))
                .ifPresent(other -> { throw new IllegalStateException("Ya existe una unidad con código " + code); });
        unit.setCode(code);
        unit.setName(request.getName().trim());
        unit.setSymbol(request.getSymbol().trim());
        return response(repository.save(unit));
    }

    @PatchMapping("/{id}/active")
    public UnitOfMeasureResponse setActive(@PathVariable Long id, @RequestParam boolean active, Authentication auth) {
        validateAdmin(auth);
        UnitOfMeasure unit = repository.findById(id).orElseThrow(() -> new IllegalArgumentException("Unidad no encontrada: " + id));
        unit.setActive(active);
        return response(repository.save(unit));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id, Authentication auth) {
        validateAdmin(auth);
        UnitOfMeasure unit = repository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Unidad no encontrada: " + id));
        if (productRepository.existsByBaseUnitId(id)) {
            throw new IllegalStateException("No se puede eliminar la unidad porque está asignada como unidad base de uno o más productos.");
        }
        if (productUnitRepository.existsByCode(unit.getCode())) {
            throw new IllegalStateException("No se puede eliminar la unidad porque está utilizada en presentaciones de productos. Primero corrige esas presentaciones.");
        }
        repository.delete(unit);
        return ResponseEntity.noContent().build();
    }

    private void validateAdmin(Authentication auth) {
        if (auth == null || !auth.isAuthenticated()) throw new IllegalStateException("No existe un usuario autenticado");
        User user = userService.getByEmail(auth.getName());
        if (user.getRole() != Role.ADMIN) throw new org.springframework.security.access.AccessDeniedException("Solo un administrador puede administrar unidades de medida");
    }

    private String normalize(String code) { return code.trim().toUpperCase(Locale.ROOT); }
    private UnitOfMeasureResponse response(UnitOfMeasure unit) {
        return UnitOfMeasureResponse.builder().id(unit.getId()).code(unit.getCode()).name(unit.getName())
                .symbol(unit.getSymbol()).active(unit.getActive()).build();
    }
}