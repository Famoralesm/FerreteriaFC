package com.ferreteria.fc.domain.service.Impl;

import com.ferreteria.fc.domain.dto.mapper.ProductMapper;
import com.ferreteria.fc.domain.dto.request.CreateProductRequest;
import com.ferreteria.fc.domain.dto.request.PatchProductRequest;
import com.ferreteria.fc.domain.dto.request.UpdateProductRequest;
import com.ferreteria.fc.domain.dto.response.ProductResponse;
import com.ferreteria.fc.domain.repository.ProductRepository;
import com.ferreteria.fc.domain.repository.BrandRepository;
import com.ferreteria.fc.persistence.entity.Brand;
import com.ferreteria.fc.domain.repository.ProductUnitRepository;
import com.ferreteria.fc.domain.repository.OrderRepository;
import com.ferreteria.fc.domain.repository.ProductStockRepository;
import com.ferreteria.fc.domain.repository.StoreRepository;
import com.ferreteria.fc.domain.repository.UnitOfMeasureRepository;
import com.ferreteria.fc.domain.service.ImageStorageService;
import com.ferreteria.fc.domain.service.ProductService;
import com.ferreteria.fc.domain.service.AuditLogService;
import com.ferreteria.fc.persistence.entity.Product;
import com.ferreteria.fc.persistence.entity.ProductStock;
import com.ferreteria.fc.persistence.entity.ProductUnit;
import com.ferreteria.fc.persistence.entity.Store;
import com.ferreteria.fc.persistence.entity.UnitOfMeasure;
import com.ferreteria.fc.domain.enums.ImageKind;
import com.ferreteria.fc.domain.service.ImageStorageService.StoredImage;

import org.springframework.web.multipart.MultipartFile;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.LinkedHashMap;
import java.util.Map;
import java.math.BigDecimal;

@Service
@Transactional
public class ProductServiceImpl implements ProductService {

    private final ProductRepository productRepository;
    private final BrandRepository brandRepository;
    private final ProductStockRepository productStockRepository;
    private final StoreRepository storeRepository;
    private final ProductMapper productMapper;
    private final ImageStorageService imageStorageService;
    private final UnitOfMeasureRepository unitOfMeasureRepository;
    private final ProductUnitRepository productUnitRepository;
    private final OrderRepository orderRepository;
    private final AuditLogService auditLogService;

    public ProductServiceImpl(
            ProductRepository productRepository,
            BrandRepository brandRepository,
            ProductStockRepository productStockRepository,
            StoreRepository storeRepository,
            ProductMapper productMapper,
            ImageStorageService imageStorageService,
            UnitOfMeasureRepository unitOfMeasureRepository,
            ProductUnitRepository productUnitRepository,
            OrderRepository orderRepository,
            AuditLogService auditLogService
    ) {
        this.imageStorageService= imageStorageService;
        this.productStockRepository = productStockRepository;
        this.brandRepository = brandRepository;
        this.storeRepository = storeRepository;
        this.productRepository = productRepository;
        this.productMapper = productMapper;
        this.unitOfMeasureRepository = unitOfMeasureRepository;
        this.productUnitRepository = productUnitRepository;
        this.orderRepository = orderRepository;
        this.auditLogService = auditLogService;
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
                .minimumSaleQuantity(request.getMinimumSaleQuantity() != null ? request.getMinimumSaleQuantity() : BigDecimal.ONE)
                .saleQuantityIncrement(request.getSaleQuantityIncrement() != null ? request.getSaleQuantityIncrement() : BigDecimal.ONE)
                .stock(0)
                .active(
                        request.getActive() != null
                                ? request.getActive()
                                : true
                )
                .category(request.getCategory())
                .brand(resolveBrandName(request.getBrandId(), request.getBrand()))
                .brandEntity(resolveBrand(request.getBrandId()))
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
                        .stock(BigDecimal.ZERO)
                        .stockMinimo(BigDecimal.ZERO)
                        .build())
                .toList();

        productStockRepository.saveAll(stocksIniciales);
        recordProductChange(saved, "CREATED", null, "Producto creado");

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

        Map<String, Object> previousValues = productAuditValues(product);

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
        product.setMinimumSaleQuantity(request.getMinimumSaleQuantity() != null ? request.getMinimumSaleQuantity() : BigDecimal.ONE);
        product.setSaleQuantityIncrement(request.getSaleQuantityIncrement() != null ? request.getSaleQuantityIncrement() : BigDecimal.ONE);
        product.setActive(request.getActive());
        product.setCategory(request.getCategory());
        if (request.getBrandId() != null) {
            Brand brand = resolveBrand(request.getBrandId());
            product.setBrandEntity(brand);
            product.setBrand(brand.getName());
        } else if (request.getBrand() != null) {
            product.setBrand(request.getBrand());
            product.setBrandEntity(null);
        }
        if (request.getUnit() != null) product.setUnit(request.getUnit());

        Product saved = productRepository.save(product);
        recordProductChange(saved,
                !Boolean.TRUE.equals(saved.getActive()) ? "DEACTIVATED" : "UPDATED",
                previousValues, "Producto actualizado");
        return productMapper.toResponse(saved);
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

        Map<String, Object> previousValues = productAuditValues(product);

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


        if (request.getMinimumSaleQuantity() != null) product.setMinimumSaleQuantity(request.getMinimumSaleQuantity());
        if (request.getSaleQuantityIncrement() != null) product.setSaleQuantityIncrement(request.getSaleQuantityIncrement());

        if (request.getActive() != null) {
            product.setActive(request.getActive());
        }

        if (request.getCategory() != null) {
            product.setCategory(request.getCategory());
        }

        if (request.getBrandId() != null) {
            Brand brand = resolveBrand(request.getBrandId());
            product.setBrandEntity(brand);
            product.setBrand(brand.getName());
        } else if (request.getBrand() != null) {
            product.setBrand(request.getBrand());
            product.setBrandEntity(null);
        }

        if (request.getUnit() != null) {
            product.setUnit(request.getUnit());
        }

        Product saved = productRepository.save(product);
        recordProductChange(saved,
                !Boolean.TRUE.equals(saved.getActive()) ? "DEACTIVATED" : "UPDATED",
                previousValues, "Producto actualizado parcialmente");
        return productMapper.toResponse(saved);
    }

    @Override
    public ProductResponse setBaseUnit(Long id, Long unitOfMeasureId, boolean preserveExistingValues) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Producto no encontrado: " + id));
        UnitOfMeasure unit = unitOfMeasureRepository.findById(unitOfMeasureId)
                .orElseThrow(() -> new IllegalArgumentException("Unidad de medida no encontrada: " + unitOfMeasureId));
        if (!Boolean.TRUE.equals(unit.getActive())) {
            throw new IllegalStateException("No puedes asignar una unidad de medida inactiva como unidad base.");
        }
        Map<String, Object> previousValues = productAuditValues(product);
        UnitOfMeasure previousBaseUnit = product.getBaseUnit();
        boolean changingBaseUnit = previousBaseUnit != null
                && !previousBaseUnit.getId().equals(unitOfMeasureId);
        if (changingBaseUnit && !preserveExistingValues) {
            throw new IllegalStateException("Para cambiar la unidad base debes confirmar que los precios, stock, reglas y factores ya están expresados en la nueva unidad.");
        }

        if (changingBaseUnit) {
            productUnitRepository.findByProductIdAndCode(id, previousBaseUnit.getCode())
                    .ifPresent(previousPresentation -> previousPresentation.setActive(false));
            // Corrige la etiqueta de líneas históricas vendidas directamente en la unidad base.
            // No se alteran cantidades ni importes ya registrados.
            orderRepository.updateBaseUnitSnapshotName(id, unit.getName());
        }

        product.setBaseUnit(unit);
        ProductUnit basePresentation = productUnitRepository.findByProductIdAndCode(id, unit.getCode())
                .orElseGet(() -> ProductUnit.builder()
                        .product(product)
                        .code(unit.getCode())
                        .name(unit.getName())
                        .conversionFactor(BigDecimal.ONE)
                        .purchaseAllowed(true)
                        .saleAllowed(true)
                        .active(true)
                        .build());
        basePresentation.setName(unit.getName());
        basePresentation.setConversionFactor(BigDecimal.ONE);
        basePresentation.setPurchaseAllowed(true);
        basePresentation.setSaleAllowed(true);
        basePresentation.setActive(true);
        productUnitRepository.save(basePresentation);
        Product saved = productRepository.save(product);
        recordProductChange(saved, "BASE_UNIT_CHANGED", previousValues,
                "Unidad base del producto actualizada");
        return productMapper.toResponse(saved);
    }

    @Override
    public void delete(Long id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Producto no encontrado: " + id
                ));

        // Los productos con historial operativo se conservan para no romper
        // movimientos, pedidos, compras ni trazabilidad de inventario.
        Map<String, Object> previousValues = productAuditValues(product);
        product.setActive(false);
        Product saved = productRepository.saveAndFlush(product);
        recordProductChange(saved, "DEACTIVATED", previousValues, "Producto desactivado");
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

    private Map<String, Object> productAuditValues(Product product) {
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("sku", product.getSku());
        values.put("name", product.getName());
        values.put("price", product.getPrice());
        values.put("wholesalePrice", product.getWholesalePrice());
        values.put("wholesaleMinQuantity", product.getWholesaleMinQuantity());
        values.put("active", product.getActive());
        values.put("category", product.getCategory());
        values.put("brand", product.getBrand());
        values.put("baseUnit", product.getBaseUnit() != null ? product.getBaseUnit().getName() : null);
        values.put("minimumSaleQuantity", product.getMinimumSaleQuantity());
        values.put("saleQuantityIncrement", product.getSaleQuantityIncrement());
        return values;
    }

    private void recordProductChange(
            Product product,
            String action,
            Map<String, Object> previousValues,
            String description
    ) {
        auditLogService.recordForCurrentUser(
                "PRODUCTS", "PRODUCT", product.getId().toString(), action, null,
                previousValues, productAuditValues(product), description, "WEB"
        );
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
                productStock != null ? productStock.getStock() : BigDecimal.ZERO
        );

        response.setStockMinimo(
                productStock != null ? productStock.getStockMinimo() : BigDecimal.ZERO
        );

        return response;
    }
    private Brand resolveBrand(Long brandId) {
        if (brandId == null) return null;
        return brandRepository.findByIdAndActiveTrue(brandId)
                .orElseThrow(() -> new IllegalArgumentException("La marca seleccionada no existe o está inactiva"));
    }

    private String resolveBrandName(Long brandId, String legacyName) {
        Brand brand = resolveBrand(brandId);
        return brand != null ? brand.getName() : legacyName;
    }

}