package com.ferreteria.fc.web.controller;

import com.ferreteria.fc.domain.dto.response.CiudadResponse;
import com.ferreteria.fc.domain.service.CiudadService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/ciudades")
public class CiudadController {

    private final CiudadService ciudadService;

    public CiudadController(CiudadService ciudadService) {
        this.ciudadService = ciudadService;
    }

    @GetMapping
    public ResponseEntity<List<CiudadResponse>> getAll() {
        return ResponseEntity.ok(
                ciudadService.getAll()
        );
    }

    @GetMapping("/{id}")
    public ResponseEntity<CiudadResponse> getById(
            @PathVariable Long id
    ) {
        return ResponseEntity.ok(
                ciudadService.getById(id)
        );
    }
}