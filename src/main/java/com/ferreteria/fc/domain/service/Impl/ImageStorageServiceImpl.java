package com.ferreteria.fc.domain.service.Impl;

import com.ferreteria.fc.domain.enums.ImageKind;
import com.ferreteria.fc.domain.service.ImageStorageService;
import com.ferreteria.fc.domain.service.ImageStorageService.StoredImage;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageInputStream;
import javax.imageio.stream.ImageOutputStream;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;

import java.security.MessageDigest;

import java.util.HexFormat;
import java.util.Iterator;
import java.util.Locale;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
public class ImageStorageServiceImpl implements ImageStorageService {

    private static final Logger log =
            LoggerFactory.getLogger(ImageStorageServiceImpl.class);

    @Value("${media.storage-root:./storage/ferreteria/media}")
    private String storageRoot;

    private static final Set<String> ALLOWED_FORMATS =
            Set.of("jpeg", "jpg", "png", "webp");


    @Override
    public StoredImage store(
            MultipartFile file,
            ImageKind kind
    ) {

        if (file == null || file.isEmpty()) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Debes seleccionar una imagen"
            );

        }

        if (kind == null) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Tipo de imagen no válido"
            );

        }

        if (file.getSize() > kind.getMaxBytes()) {

            throw new ResponseStatusException(
                    HttpStatus.PAYLOAD_TOO_LARGE,
                    "La imagen supera el tamaño permitido"
            );

        }

        try {

            byte[] originalBytes = file.getBytes();

            BufferedImage original = decodeImage(originalBytes);

            BufferedImage resized = resizeImage(
                    original,
                    kind.getMaxDimension(),
                    kind.isSquare()
            );

            byte[] webp = encodeWebp(resized);

            String hash = sha256(webp);

            String relativePath =
                    kind.getFolder() + "/" + hash + ".webp";

            Path destination = resolve(relativePath);

            Files.createDirectories(destination.getParent());

            if (!Files.exists(destination)) {

                try {

                    Files.write(
                            destination,
                            webp,
                            StandardOpenOption.CREATE_NEW
                    );

                } catch (
                        java.nio.file.FileAlreadyExistsException ignored
                ) {

                    // El archivo ya fue creado por otra operación
                    // que almacenó exactamente la misma imagen.

                }

            }

            return new StoredImage(
                    relativePath,
                    hash,
                    webp.length,
                    resized.getWidth(),
                    resized.getHeight()
            );

        } catch (ResponseStatusException ex) {

            throw ex;

        } catch (Exception ex) {

            log.error(
                    "ERROR PROCESANDO IMAGEN | Tipo: {} | Content-Type: {} | Tamaño: {} bytes",
                    kind,
                    file.getContentType(),
                    file.getSize(),
                    ex
            );

            throw new ResponseStatusException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "No fue posible procesar la imagen",
                    ex
            );

        }

    }


    private BufferedImage decodeImage(
            byte[] bytes
    ) throws IOException {

        try (
                ImageInputStream input =
                        ImageIO.createImageInputStream(
                                new ByteArrayInputStream(bytes)
                        )
        ) {

            if (input == null) {
                throw invalidImage();
            }

            Iterator<ImageReader> readers =
                    ImageIO.getImageReaders(input);

            if (!readers.hasNext()) {
                throw invalidImage();
            }

            ImageReader reader = readers.next();

            try {

                reader.setInput(input, true, true);

                String format = reader
                        .getFormatName()
                        .toLowerCase(Locale.ROOT);

                if (!ALLOWED_FORMATS.contains(format)) {
                    throw invalidImage();
                }

                int width = reader.getWidth(0);
                int height = reader.getHeight(0);

                long pixels = (long) width * height;

                if (
                        width <= 0 ||
                                height <= 0 ||
                                width > 8000 ||
                                height > 8000 ||
                                pixels > 25_000_000L
                ) {

                    throw new ResponseStatusException(
                            HttpStatus.BAD_REQUEST,
                            "Las dimensiones de la imagen son demasiado grandes"
                    );

                }

                BufferedImage image = reader.read(0);

                if (image == null) {
                    throw invalidImage();
                }

                return image;

            } finally {

                reader.dispose();

            }

        }

    }


    private BufferedImage resizeImage(
            BufferedImage source,
            int maxDimension,
            boolean square
    ) {

        boolean alpha =
                source.getColorModel().hasAlpha();

        int imageType = alpha
                ? BufferedImage.TYPE_INT_ARGB
                : BufferedImage.TYPE_INT_RGB;

        int targetWidth;
        int targetHeight;

        if (square) {

            targetWidth = maxDimension;
            targetHeight = maxDimension;

        } else {

            double scale = Math.min(
                    1.0,
                    (double) maxDimension /
                            Math.max(
                                    source.getWidth(),
                                    source.getHeight()
                            )
            );

            targetWidth = Math.max(
                    1,
                    (int) Math.round(source.getWidth() * scale)
            );

            targetHeight = Math.max(
                    1,
                    (int) Math.round(source.getHeight() * scale)
            );

        }

        BufferedImage result = new BufferedImage(
                targetWidth,
                targetHeight,
                imageType
        );

        Graphics2D graphics = result.createGraphics();

        try {

            graphics.setRenderingHint(
                    RenderingHints.KEY_INTERPOLATION,
                    RenderingHints.VALUE_INTERPOLATION_BICUBIC
            );

            graphics.setRenderingHint(
                    RenderingHints.KEY_RENDERING,
                    RenderingHints.VALUE_RENDER_QUALITY
            );

            if (square) {

                double scale = Math.max(
                        (double) maxDimension / source.getWidth(),
                        (double) maxDimension / source.getHeight()
                );

                int scaledWidth = (int) Math.ceil(
                        source.getWidth() * scale
                );

                int scaledHeight = (int) Math.ceil(
                        source.getHeight() * scale
                );

                int x = (scaledWidth - maxDimension) / 2;
                int y = (scaledHeight - maxDimension) / 2;

                graphics.drawImage(
                        source,
                        -x,
                        -y,
                        scaledWidth,
                        scaledHeight,
                        null
                );

            } else {

                graphics.drawImage(
                        source,
                        0,
                        0,
                        targetWidth,
                        targetHeight,
                        null
                );

            }

        } finally {

            graphics.dispose();

        }

        return result;

    }



    private byte[] encodeWebp(BufferedImage image) throws IOException {

        Iterator<ImageWriter> writers =
                ImageIO.getImageWritersByMIMEType("image/webp");

        if (!writers.hasNext()) {
            throw new IllegalStateException(
                    "No está disponible el codificador WebP. " +
                            "Revisa la dependencia webp-imageio."
            );
        }

        ImageWriter writer = writers.next();

        try (ByteArrayOutputStream bytes = new ByteArrayOutputStream()) {

            ImageOutputStream output =
                    ImageIO.createImageOutputStream(bytes);

            if (output == null) {
                throw new IOException(
                        "No fue posible crear el flujo de salida WebP"
                );
            }

            try (output) {

                // Asociar el escritor al flujo de salida.
                writer.setOutput(output);

                ImageWriteParam writeParam =
                        writer.getDefaultWriteParam();

                if (writeParam.canWriteCompressed()) {

                    writeParam.setCompressionMode(
                            ImageWriteParam.MODE_EXPLICIT
                    );

                    String[] compressionTypes =
                            writeParam.getCompressionTypes();

                    if (compressionTypes != null &&
                            compressionTypes.length > 0) {

                        String compressionType =
                                java.util.Arrays.stream(compressionTypes)
                                        .filter("Lossy"::equalsIgnoreCase)
                                        .findFirst()
                                        .orElse(compressionTypes[0]);

                        writeParam.setCompressionType(compressionType);
                        writeParam.setCompressionQuality(0.85f);
                    }
                }

                // ESTA ES LA OPERACIÓN QUE FALTABA.
                writer.write(
                        null,
                        new IIOImage(image, null, null),
                        writeParam
                );

                output.flush();
            }

            byte[] result = bytes.toByteArray();

            if (result.length == 0) {
                throw new IOException(
                        "El codificador WebP generó un archivo vacío"
                );
            }

            return result;

        } finally {
            writer.dispose();
        }
    }



    private String sha256(
            byte[] bytes
    ) throws Exception {

        MessageDigest digest =
                MessageDigest.getInstance("SHA-256");

        return HexFormat.of().formatHex(
                digest.digest(bytes)
        );

    }


    private ResponseStatusException invalidImage() {

        return new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "El archivo no es una imagen JPEG, PNG o WebP válida"
        );

    }


    @Override
    public Path resolve(String relativePath) {

        Path root = Paths.get(storageRoot)
                .toAbsolutePath()
                .normalize();

        Path destination = root
                .resolve(relativePath)
                .normalize();

        if (!destination.startsWith(root)) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Ruta de imagen no válida"
            );

        }

        return destination;

    }


    @Override
    public void delete(String relativePath) {

        if (relativePath == null || relativePath.isBlank()) {
            return;
        }

        try {

            Files.deleteIfExists(
                    resolve(relativePath)
            );

        } catch (IOException ex) {

            throw new ResponseStatusException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "No fue posible eliminar la imagen almacenada",
                    ex
            );

        }

    }


    @Override
    public String publicUrl(String relativePath) {

        return relativePath == null
                ? null
                : "/api/media/" + relativePath;

    }

}