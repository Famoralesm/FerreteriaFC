package com.ferreteria.fc.web.controller;

import com.ferreteria.fc.domain.dto.request.BrandRequest;
import com.ferreteria.fc.domain.dto.response.BrandResponse;
import com.ferreteria.fc.domain.repository.BrandRepository;
import com.ferreteria.fc.persistence.entity.Brand;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/brands")
@CrossOrigin(origins = "http://localhost:4200")
public class BrandController {
    private final BrandRepository brandRepository;

    public BrandController(BrandRepository brandRepository) {
        this.brandRepository = brandRepository;
    }

    @GetMapping
    public ResponseEntity<List<BrandResponse>> getAll() {
        return ResponseEntity.ok(brandRepository.findByActiveTrueOrderByNameAsc()
                .stream().map(this::toResponse).toList());
    }

    @PostMapping
    public ResponseEntity<BrandResponse> create(@Valid @RequestBody BrandRequest request) {
        String name = request.getName().trim().replaceAll("\\s+", " ");
        if (brandRepository.existsByNameIgnoreCase(name)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Ya existe una marca con ese nombre.");
        }
        Brand saved = brandRepository.save(Brand.builder().name(name).active(true).build());
        return ResponseEntity.ok(toResponse(saved));
    }

    private BrandResponse toResponse(Brand brand) {
        return BrandResponse.builder().id(brand.getId()).name(brand.getName()).build();
    }
}
