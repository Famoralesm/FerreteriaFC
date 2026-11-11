package com.ferreteria.fc.domain.service.Impl;

import com.ferreteria.fc.domain.dto.mapper.ProductMapper;
import com.ferreteria.fc.domain.dto.request.CreateProductRequest;
import com.ferreteria.fc.domain.dto.request.PatchProductRequest;
import com.ferreteria.fc.domain.dto.request.UpdateProductRequest;
import com.ferreteria.fc.domain.dto.response.ProductResponse;
import com.ferreteria.fc.domain.repository.ProductRepository;
import com.ferreteria.fc.domain.service.ImageStorageService;
import com.ferreteria.fc.domain.service.ProductService;
import com.ferreteria.fc.persistence.entity.Product;
import com.ferreteria.fc.domain.enums.ImageKind;
import com.ferreteria.fc.domain.service.ImageStorageService.StoredImage;

import org.springframework.web.multipart.MultipartFile;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class ProductServiceImpl implements ProductService {

    private final ProductRepository productRepository;
    private final ProductMapper productMapper;
    private final ImageStorageService imageStorageService;

    public ProductServiceImpl(
            ProductRepository productRepository,
            ProductMapper productMapper,
            ImageStorageService imageStorageService
    ) {
        this.imageStorageService= imageStorageService;
        this.productRepository = productRepository;
        this.productMapper = productMapper;
    }

    @Override
    public ProductResponse createProduct(
            CreateProductRequest request
    ) {

        if (productRepository.existsBySku(request.getSku())) {
            throw new RuntimeException(
                    "Ya existe un producto con SKU: "
                            + request.getSku()
            );
        }

        Product product = Product.builder()
                .sku(request.getSku())
                .name(request.getName())
                .description(request.getDescription())
                .price(request.getPrice())
                .stock(request.getStock())
                .active(
                        request.getActive() != null
                                ? request.getActive()
                                : true
                )
                .category(request.getCategory())
                .brand(request.getBrand())
                .unit(request.getUnit())
                .build();

        Product saved = productRepository.save(product);

        return productMapper.toResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProductResponse> getAll() {

        return productMapper.toListResponse(
                productRepository.findAll()
        );
    }

    @Override
    @Transactional(readOnly = true)
    public ProductResponse getById(Long id) {

        Product product = productRepository.findById(id)
                .orElseThrow(() ->
                        new RuntimeException(
                                "Producto no encontrado: " + id
                        )
                );

        return productMapper.toResponse(product);
    }

    @Override
    public ProductResponse updateProduct(
            Long id,
            UpdateProductRequest request
    ) {

        Product product = productRepository.findById(id)
                .orElseThrow(() ->
                        new RuntimeException(
                                "Producto no encontrado: " + id
                        )
                );

        /*
         * PUT = reemplazo completo
         */

        if (!product.getSku().equals(request.getSku())
                && productRepository.existsBySku(request.getSku())) {

            throw new RuntimeException(
                    "Ya existe un producto con SKU: "
                            + request.getSku()
            );
        }

        product.setSku(request.getSku());
        product.setName(request.getName());
        product.setDescription(request.getDescription());
        product.setPrice(request.getPrice());
        product.setStock(request.getStock());
        product.setActive(request.getActive());
        product.setCategory(request.getCategory());
        product.setBrand(request.getBrand());
        product.setUnit(request.getUnit());

        return productMapper.toResponse(
                productRepository.save(product)
        );
    }

    @Override
    public ProductResponse patchProduct(
            Long id,
            PatchProductRequest request
    ) {

        Product product = productRepository.findById(id)
                .orElseThrow(() ->
                        new RuntimeException(
                                "Producto no encontrado: " + id
                        )
                );

        /*
         * PATCH = solamente modificar
         * lo que venga informado.
         */

        if (request.getSku() != null) {

            if (!product.getSku().equals(request.getSku())
                    && productRepository.existsBySku(request.getSku())) {

                throw new RuntimeException(
                        "Ya existe un producto con SKU: "
                                + request.getSku()
                );
            }

            product.setSku(request.getSku());
        }

        if (request.getName() != null) {
            product.setName(request.getName());
        }

        if (request.getDescription() != null) {
            product.setDescription(request.getDescription());
        }

        if (request.getPrice() != null) {
            product.setPrice(request.getPrice());
        }

        if (request.getStock() != null) {
            product.setStock(request.getStock());
        }

        if (request.getActive() != null) {
            product.setActive(request.getActive());
        }

        if (request.getCategory() != null) {
            product.setCategory(request.getCategory());
        }

        if (request.getBrand() != null) {
            product.setBrand(request.getBrand());
        }

        if (request.getUnit() != null) {
            product.setUnit(request.getUnit());
        }

        return productMapper.toResponse(
                productRepository.save(product)
        );
    }

    @Override
    public void delete(Long id) {

        Product product = productRepository.findById(id)
                .orElseThrow(() ->
                        new RuntimeException(
                                "Producto no encontrado: " + id
                        )
                );
        String previousPath = product.getImagePath();

        productRepository.delete(product);

        productRepository.flush();

        deleteUnusedProductImage(previousPath, null);
    }

    @Override
    public List<Product> search(String query) {

        if (query == null || query.isBlank()) {
            return List.of();
        }

        return productRepository
                .findBySkuContainingIgnoreCaseOrNameContainingIgnoreCase(
                        query,
                        query
                );
    }

    @Override
    public ProductResponse updateProductImage(
            Long id,
            MultipartFile file
    ) {

        Product product = productRepository.findById(id)
                .orElseThrow(() ->
                        new RuntimeException(
                                "Producto no encontrado: " + id
                        )
                );

        String previousPath = product.getImagePath();

        StoredImage image = imageStorageService.store(
                file,
                ImageKind.PRODUCT_IMAGE
        );

        product.setImagePath(image.relativePath());
        product.setImageSha256(image.sha256());
        product.setImageSizeBytes(image.sizeBytes());
        product.setImageWidth(image.width());
        product.setImageHeight(image.height());

        Product saved = productRepository.saveAndFlush(product);

        deleteUnusedProductImage(previousPath, saved.getImagePath());

        return productMapper.toResponse(saved);

    }


    @Override
    public ProductResponse deleteProductImage(Long id) {

        Product product = productRepository.findById(id)
                .orElseThrow(() ->
                        new RuntimeException(
                                "Producto no encontrado: " + id
                        )
                );

        String previousPath = product.getImagePath();

        product.setImagePath(null);
        product.setImageSha256(null);
        product.setImageSizeBytes(null);
        product.setImageWidth(null);
        product.setImageHeight(null);

        Product saved = productRepository.saveAndFlush(product);

        deleteUnusedProductImage(previousPath, null);

        return productMapper.toResponse(saved);

    }


    private void deleteUnusedProductImage(
            String previousPath,
            String currentPath
    ) {

        if (
                previousPath != null &&
                        !previousPath.equals(currentPath) &&
                        productRepository.countByImagePath(previousPath) == 0
        ) {

            imageStorageService.delete(previousPath);

        }

    }
}