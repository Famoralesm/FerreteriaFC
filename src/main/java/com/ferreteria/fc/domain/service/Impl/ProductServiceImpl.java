package com.ferreteria.fc.domain.service.Impl;

import com.ferreteria.fc.domain.dto.mapper.ProductMapper;
import com.ferreteria.fc.domain.dto.request.CreateProductRequest;
import com.ferreteria.fc.domain.dto.request.PatchProductRequest;
import com.ferreteria.fc.domain.dto.request.UpdateProductRequest;
import com.ferreteria.fc.domain.dto.response.ProductResponse;
import com.ferreteria.fc.domain.repository.ProductRepository;
import com.ferreteria.fc.domain.repository.ProductStockRepository;
import com.ferreteria.fc.domain.repository.StoreRepository;
import com.ferreteria.fc.domain.service.ImageStorageService;
import com.ferreteria.fc.domain.service.ProductService;
import com.ferreteria.fc.persistence.entity.Product;
import com.ferreteria.fc.persistence.entity.ProductStock;
import com.ferreteria.fc.persistence.entity.Store;
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
    private final ProductStockRepository productStockRepository;
    private final StoreRepository storeRepository;
    private final ProductMapper productMapper;
    private final ImageStorageService imageStorageService;

    public ProductServiceImpl(
            ProductRepository productRepository,
            ProductStockRepository productStockRepository,
            StoreRepository storeRepository,
            ProductMapper productMapper,
            ImageStorageService imageStorageService
    ) {
        this.imageStorageService= imageStorageService;
        this.productStockRepository = productStockRepository;
        this.storeRepository = storeRepository;
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
                .wholesalePrice(request.getWholesalePrice())
                .wholesaleMinQuantity(request.getWholesaleMinQuantity())
                .stock(0)
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

        List<Store> sucursales =
                storeRepository.findByActivaTrueOrderByNombreAsc();

        if (sucursales.isEmpty()) {
            throw new IllegalStateException(
                    "No existen sucursales activas para inicializar el inventario"
            );
        }

        List<ProductStock> stocksIniciales = sucursales.stream()
                .map(sucursal -> ProductStock.builder()
                        .producto(saved)
                        .sucursal(sucursal)
                        .stock(0)
                        .stockMinimo(0)
                        .build())
                .toList();

        productStockRepository.saveAll(stocksIniciales);

        return productMapper.toResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProductResponse> getAll(Long sucursalId) {

        return productStockRepository
                .findAllBySucursalIdWithProduct(sucursalId)
                .stream()
                .map(ps -> {
                    ProductResponse response =
                            productMapper.toResponse(ps.getProducto());

                    response.setStock(ps.getStock());
                    response.setStock(ps.getStock());
                    response.setStockMinimo(ps.getStockMinimo());
                    return response;
                })
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public ProductResponse getById(Long id, Long sucursalId) {

        Product product = productRepository.findById(id)
                .orElseThrow(() ->
                        new RuntimeException(
                                "Producto no encontrado: " + id
                        )
                );

        return toResponseWithStock(product, sucursalId);
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
        product.setWholesalePrice(request.getWholesalePrice());
        product.setWholesaleMinQuantity(request.getWholesaleMinQuantity());
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

        if (request.getWholesalePrice() != null) {
            product.setWholesalePrice(request.getWholesalePrice());
        }

        if (request.getWholesaleMinQuantity() != null) {
            product.setWholesaleMinQuantity(request.getWholesaleMinQuantity());
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
    @Transactional(readOnly = true)
    public List<ProductResponse> search(
            String query,
            Long sucursalId
    ) {

        if (query == null || query.isBlank()) {
            return List.of();
        }

        return productStockRepository
                .searchBySucursalAndProduct(
                        sucursalId,
                        query.trim()
                )
                .stream()
                .map(ps -> {

                    ProductResponse response =
                            productMapper.toResponse(ps.getProducto());

                    response.setStock(ps.getStock());
                    response.setStock(ps.getStock());
                    response.setStockMinimo(ps.getStockMinimo());
                    return response;
                })
                .toList();
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

    private ProductResponse toResponseWithStock(
            Product product,
            Long sucursalId
    ) {
        ProductResponse response = productMapper.toResponse(product);

        ProductStock productStock = productStockRepository
                .findByProductoIdAndSucursalId(
                        product.getId(),
                        sucursalId
                )
                .orElse(null);

        response.setStock(
                productStock != null ? productStock.getStock() : 0
        );

        response.setStockMinimo(
                productStock != null ? productStock.getStockMinimo() : 0
        );

        return response;
    }
}