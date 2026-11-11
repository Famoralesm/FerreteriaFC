package com.ferreteria.fc.domain.service;

import com.ferreteria.fc.domain.enums.ImageKind;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Path;

public interface ImageStorageService {

    record StoredImage(
            String relativePath,
            String sha256,
            long sizeBytes,
            int width,
            int height
    ) {}

    StoredImage store(
            MultipartFile file,
            ImageKind kind
    );

    Path resolve(String relativePath);

    void delete(String relativePath);

    String publicUrl(String relativePath);

}