package com.ferreteria.fc.web.controller;

import com.ferreteria.fc.domain.service.ImageStorageService;

import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;

import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;

import java.util.Set;

@RestController
@RequestMapping("/api/media")
public class MediaController {

    private final ImageStorageService imageStorageService;

    private static final Set<String> ALLOWED_FOLDERS =
            Set.of("users", "products");

    public MediaController(
            ImageStorageService imageStorageService
    ) {
        this.imageStorageService = imageStorageService;
    }

    @GetMapping("/{folder}/{filename:.+}")
    public ResponseEntity<Resource> getImage(
            @PathVariable String folder,
            @PathVariable String filename
    ) {

        if (
                !ALLOWED_FOLDERS.contains(folder) ||
                        !filename.matches("[a-f0-9]{64}\\.webp")
        ) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "Imagen no encontrada"
            );
        }

        Path path = imageStorageService.resolve(
                folder + "/" + filename
        );

        if (!Files.isRegularFile(path)) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "Imagen no encontrada"
            );
        }

        return ResponseEntity.ok()

                .contentType(MediaType.parseMediaType("image/webp"))

                .cacheControl(
                        CacheControl.maxAge(Duration.ofDays(30))
                                .cachePublic()
                )

                .body(new FileSystemResource(path));

    }

}