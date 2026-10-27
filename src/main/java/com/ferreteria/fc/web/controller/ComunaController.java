package com.ferreteria.fc.web.controller;

import com.ferreteria.fc.domain.dto.response.ComunaResponse;
import com.ferreteria.fc.domain.service.ComunaService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/comunas")
public class ComunaController {

    private final ComunaService comunaService;

    public ComunaController(ComunaService comunaService) {
        this.comunaService = comunaService;
    }

    @GetMapping
    public ResponseEntity<List<ComunaResponse>> getAll() {
        return ResponseEntity.ok(
                comunaService.getAll()
        );
    }

    @GetMapping("/{id}")
    public ResponseEntity<ComunaResponse> getById(
            @PathVariable Long id
    ) {
        return ResponseEntity.ok(
                comunaService.getById(id)
        );
    }
}