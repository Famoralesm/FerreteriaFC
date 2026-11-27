package com.ferreteria.fc.domain.service;

import com.ferreteria.fc.domain.dto.response.AuditLogResponse;
import com.ferreteria.fc.domain.repository.AuditLogRepository;
import com.ferreteria.fc.domain.repository.CustomerRepository;
import com.ferreteria.fc.domain.repository.SupplierRepository;
import com.ferreteria.fc.domain.enums.Role;
import com.ferreteria.fc.persistence.entity.Customer;
import com.ferreteria.fc.persistence.entity.Supplier;
import com.ferreteria.fc.domain.repository.OrderRepository;
import com.ferreteria.fc.domain.repository.ProductRepository;
import com.ferreteria.fc.domain.repository.UserRepository;
import com.ferreteria.fc.persistence.entity.AuditLog;
import com.ferreteria.fc.persistence.entity.Store;
import com.ferreteria.fc.persistence.entity.User;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import jakarta.persistence.criteria.Predicate;
import java.util.ArrayList;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;
import java.util.Set;

@Service
public class AuditLogService {

    private static final Set<String> SENSITIVE_KEYS = Set.of(
            "password", "contrasena", "token", "access_token",
            "refresh_token", "secret", "authorization", "credential"
    );

    private final AuditLogRepository repository;
    private final UserRepository userRepository;
    private final OrderRepository orderRepository;
    private final ProductRepository productRepository;
    private final CustomerRepository customerRepository;
    private final SupplierRepository supplierRepository;

    public AuditLogService(
            AuditLogRepository repository,
            UserRepository userRepository,
            OrderRepository orderRepository,
            ProductRepository productRepository,
            CustomerRepository customerRepository,
            SupplierRepository supplierRepository
    ) {
        this.repository = repository;
        this.userRepository = userRepository;
        this.orderRepository = orderRepository;
        this.productRepository = productRepository;
        this.customerRepository = customerRepository;
        this.supplierRepository = supplierRepository;
    }

    /**
     * Debe invocarse dentro de la transacción de negocio para que ambos cambios
     * se confirmen o reviertan juntos.
     */
    @Transactional(propagation = Propagation.MANDATORY)
    public AuditLog record(
            String module,
            String entityType,
            String entityId,
            String action,
            User actor,
            Store store,
            Map<String, Object> oldValues,
            Map<String, Object> newValues,
            String description,
            String origin
    ) {
        if (module == null || module.isBlank()
                || entityType == null || entityType.isBlank()
                || action == null || action.isBlank()) {
            throw new IllegalArgumentException("Módulo, entidad y acción son obligatorios para auditar.");
        }

        AuditLog event = new AuditLog();
        event.setUser(actor);
        event.setUserNameSnapshot(actor != null ? actor.getName() : "Proceso automático");
        event.setStore(store);
        event.setStoreNameSnapshot(store != null ? store.getNombre() : null);
        event.setModule(module.trim().toUpperCase(Locale.ROOT));
        event.setEntityType(entityType.trim().toUpperCase(Locale.ROOT));
        event.setEntityId(entityId);
        event.setAction(action.trim().toUpperCase(Locale.ROOT));
        event.setOldValues(sanitize(oldValues));
        event.setNewValues(sanitize(newValues));
        event.setDescription(description);
        event.setOrigin(origin == null || origin.isBlank() ? "SYSTEM" : origin.trim().toUpperCase(Locale.ROOT));
        return repository.save(event);
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public AuditLog recordForCurrentUser(
            String module,
            String entityType,
            String entityId,
            String action,
            Store store,
            Map<String, Object> oldValues,
            Map<String, Object> newValues,
            String description,
            String origin
    ) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        User actor = authentication == null || !authentication.isAuthenticated()
                ? null
                : userRepository.findByEmail(authentication.getName());
        Store effectiveStore = store != null ? store : actor != null ? actor.getSucursal() : null;
        return record(module, entityType, entityId, action, actor, effectiveStore,
                oldValues, newValues, description, origin);
    }

    @Transactional(readOnly = true)
    public Page<AuditLogResponse> search(
            String module,
            String entityType,
            String entityId,
            String orderNumber,
            String productQuery,
            String productSku,
            Long userId,
            String userName,
            String customerRut, String customerQuery, String customerCommune, String customerCity,
            String userQuery, String userRole,
            String supplierQuery, String supplierRut, String supplierCommune, String supplierCity,
            Long storeId,
            String action,
            LocalDateTime fromDate,
            LocalDateTime toDate,
            Pageable pageable
    ) {
        if (fromDate != null && toDate != null && fromDate.isAfter(toDate)) {
            throw new IllegalArgumentException("La fecha inicial no puede ser posterior a la fecha final.");
        }

        String resolvedOrderId = null;
        if (orderNumber != null && !orderNumber.isBlank()) {
            resolvedOrderId = orderRepository.findByOrderNumber(orderNumber.trim().toUpperCase(Locale.ROOT))
                    .map(order -> order.getId().toString())
                    .orElse("__NO_MATCHING_ORDER__");
        }

        List<String> matchingProductIds = null;
        if ((productQuery != null && !productQuery.isBlank())
                || (productSku != null && !productSku.isBlank())) {
            java.util.Set<String> ids = null;
            if (productQuery != null && !productQuery.isBlank()) {
                ids = productRepository.findBySkuContainingIgnoreCaseOrNameContainingIgnoreCase(
                                productQuery.trim(), productQuery.trim())
                        .stream()
                        .map(product -> product.getId().toString())
                        .collect(java.util.stream.Collectors.toCollection(java.util.LinkedHashSet::new));
            }
            if (productSku != null && !productSku.isBlank()) {
                java.util.Set<String> skuIds = productRepository.findBySkuContainingIgnoreCase(productSku.trim())
                        .stream()
                        .map(product -> product.getId().toString())
                        .collect(java.util.stream.Collectors.toCollection(java.util.LinkedHashSet::new));
                if (ids == null) {
                    ids = skuIds;
                } else {
                    ids.retainAll(skuIds);
                }
            }
            matchingProductIds = ids == null ? List.of() : List.copyOf(ids);
            if (matchingProductIds.isEmpty()) {
                return Page.empty(pageable);
            }
        }

        List<String> customerIds = resolveEntityIds(
                collectIds(customerRut, customerRut == null || customerRut.isBlank() ? List.of() : customerRepository.findByRutContainingIgnoreCase(safe(customerRut)), Customer::getId),
                collectIds(customerQuery, customerQuery == null || customerQuery.isBlank() ? List.of() : customerRepository.findByNombreContainingIgnoreCase(safe(customerQuery)), Customer::getId),
                collectIds(customerCommune, customerCommune == null || customerCommune.isBlank() ? List.of() : customerRepository.findByComunaNombreContainingIgnoreCase(safe(customerCommune)), Customer::getId),
                collectIds(customerCity, customerCity == null || customerCity.isBlank() ? List.of() : customerRepository.findByCiudadNombreContainingIgnoreCase(safe(customerCity)), Customer::getId)
        );
        List<String> userIds = resolveEntityIds(
                collectIds(userQuery, userQuery == null || userQuery.isBlank() ? List.of() : userRepository.findByNameContainingIgnoreCase(safe(userQuery)), User::getId),
                collectIds(userRole, parseRoleUsers(userRole), User::getId)
        );
        List<String> supplierIds = resolveEntityIds(
                collectIds(supplierQuery, supplierQuery == null || supplierQuery.isBlank() ? List.of() : supplierRepository.findByNombreContainingIgnoreCase(safe(supplierQuery)), Supplier::getId),
                collectIds(supplierRut, supplierRut == null || supplierRut.isBlank() ? List.of() : supplierRepository.findByRutContainingIgnoreCase(safe(supplierRut)), Supplier::getId),
                collectIds(supplierCommune, supplierCommune == null || supplierCommune.isBlank() ? List.of() : supplierRepository.findByComunaNombreContainingIgnoreCase(safe(supplierCommune)), Supplier::getId),
                collectIds(supplierCity, supplierCity == null || supplierCity.isBlank() ? List.of() : supplierRepository.findByCiudadNombreContainingIgnoreCase(safe(supplierCity)), Supplier::getId)
        );
        if (customerIds != null && customerIds.isEmpty() || userIds != null && userIds.isEmpty()
                || supplierIds != null && supplierIds.isEmpty()) return Page.empty(pageable);

        final String orderIdFilter = resolvedOrderId;
        final List<String> productIdFilter = matchingProductIds;
        final List<String> customerIdFilter = customerIds;
        final List<String> userIdFilter = userIds;
        final List<String> supplierIdFilter = supplierIds;
        Specification<AuditLog> specification = (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();

            String normalizedModule = normalize(module);
            String normalizedEntityType = normalize(entityType);
            String normalizedEntityId = normalizeIdentifier(entityId);
            String normalizedAction = normalize(action);

            if (normalizedModule != null) {
                predicates.add(criteriaBuilder.equal(root.get("module"), normalizedModule));
            }
            if (normalizedEntityType != null) {
                predicates.add(criteriaBuilder.equal(root.get("entityType"), normalizedEntityType));
            }
            if (normalizedEntityId != null) {
                predicates.add(criteriaBuilder.equal(root.get("entityId"), normalizedEntityId));
            }
            if (orderIdFilter != null) {
                predicates.add(criteriaBuilder.equal(root.get("entityId"), orderIdFilter));
            }
            if (productIdFilter != null) predicates.add(root.get("entityId").in(productIdFilter));
            if (customerIdFilter != null) predicates.add(root.get("entityId").in(customerIdFilter));
            if (userIdFilter != null) predicates.add(root.get("entityId").in(userIdFilter));
            if (supplierIdFilter != null) predicates.add(root.get("entityId").in(supplierIdFilter));
            if (userId != null) {
                predicates.add(criteriaBuilder.equal(root.get("user").get("id"), userId));
            }
            if (userName != null && !userName.isBlank()) {
                predicates.add(criteriaBuilder.like(
                        criteriaBuilder.lower(root.get("userNameSnapshot")),
                        "%" + userName.trim().toLowerCase(Locale.ROOT) + "%"));
            }
            if (storeId != null) {
                predicates.add(criteriaBuilder.equal(root.get("store").get("id"), storeId));
            }
            if (normalizedAction != null) {
                predicates.add(criteriaBuilder.equal(root.get("action"), normalizedAction));
            }
            if (fromDate != null) {
                predicates.add(criteriaBuilder.greaterThanOrEqualTo(root.get("createdAt"), fromDate));
            }
            if (toDate != null) {
                predicates.add(criteriaBuilder.lessThanOrEqualTo(root.get("createdAt"), toDate));
            }

            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };

        return repository.findAll(specification, pageable).map(this::toResponse);
    }

    private String safe(String value) { return value == null ? "" : value.trim(); }

    private <T> List<String> collectIds(String filter, List<T> values, Function<T, ?> idExtractor) {
        if (filter == null || filter.isBlank()) return null;
        return values.stream().map(value -> String.valueOf(idExtractor.apply(value))).toList();
    }

    @SafeVarargs
    private final List<String> resolveEntityIds(List<String>... criteria) {
        List<java.util.Set<String>> active = java.util.Arrays.stream(criteria)
                .filter(java.util.Objects::nonNull)
                .<java.util.Set<String>>map(java.util.LinkedHashSet::new).toList();
        if (active.isEmpty()) return null;
        java.util.Set<String> result = new java.util.LinkedHashSet<>(active.get(0));
        active.stream().skip(1).forEach(result::retainAll);
        return List.copyOf(result);
    }

    private List<com.ferreteria.fc.persistence.entity.User> parseRoleUsers(String role) {
        if (role == null || role.isBlank()) return List.of();
        try { return userRepository.findByRole(Role.valueOf(role.trim().toUpperCase(Locale.ROOT))); }
        catch (IllegalArgumentException ex) { return List.of(); }
    }

    private AuditLogResponse toResponse(AuditLog event) {
        return new AuditLogResponse(
                event.getId(),
                event.getEventUuid(),
                event.getUser() != null ? event.getUser().getId() : null,
                event.getUserNameSnapshot(),
                event.getStore() != null ? event.getStore().getId() : null,
                event.getStoreNameSnapshot(),
                event.getModule(),
                event.getEntityType(),
                event.getEntityId(),
                event.getAction(),
                event.getOldValues(),
                event.getNewValues(),
                event.getDescription(),
                event.getOrigin(),
                event.getCreatedAt()
        );
    }

    private String normalize(String value) {
        return value == null || value.isBlank() ? null : value.trim().toUpperCase(Locale.ROOT);
    }

    private String normalizeIdentifier(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private Map<String, Object> sanitize(Map<String, Object> values) {
        if (values == null || values.isEmpty()) return null;
        Map<String, Object> safe = sanitizeMap(values);
        return safe.isEmpty() ? null : safe;
    }

    private Map<String, Object> sanitizeMap(Map<?, ?> values) {
        Map<String, Object> safe = new LinkedHashMap<>();
        values.forEach((rawKey, value) -> {
            if (rawKey == null) return;
            String key = rawKey.toString();
            String normalizedKey = key.toLowerCase(Locale.ROOT);
            boolean sensitive = SENSITIVE_KEYS.stream().anyMatch(normalizedKey::contains);
            if (!sensitive) safe.put(key, sanitizeValue(value));
        });
        return safe;
    }

    private Object sanitizeValue(Object value) {
        if (value instanceof Map<?, ?> map) return sanitizeMap(map);
        if (value instanceof Iterable<?> iterable) {
            List<Object> safe = new ArrayList<>();
            iterable.forEach(item -> safe.add(sanitizeValue(item)));
            return safe;
        }
        return value;
    }
}
