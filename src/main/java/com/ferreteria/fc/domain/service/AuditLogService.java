package com.ferreteria.fc.domain.service;

import com.ferreteria.fc.domain.dto.response.AuditLogResponse;
import com.ferreteria.fc.domain.repository.AuditLogRepository;
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

    public AuditLogService(
            AuditLogRepository repository,
            UserRepository userRepository,
            OrderRepository orderRepository,
            ProductRepository productRepository
    ) {
        this.repository = repository;
        this.userRepository = userRepository;
        this.orderRepository = orderRepository;
        this.productRepository = productRepository;
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
            Long userId,
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
            resolvedOrderId = orderRepository.findByOrderNumber(orderNumber.trim())
                    .map(order -> order.getId().toString())
                    .orElse("__NO_MATCHING_ORDER__");
        }

        List<String> matchingProductIds = null;
        if (productQuery != null && !productQuery.isBlank()) {
            matchingProductIds = productRepository
                    .findBySkuContainingIgnoreCaseOrNameContainingIgnoreCase(
                            productQuery.trim(), productQuery.trim())
                    .stream()
                    .map(product -> product.getId().toString())
                    .toList();
            if (matchingProductIds.isEmpty()) {
                return Page.empty(pageable);
            }
        }

        final String orderIdFilter = resolvedOrderId;
        final List<String> productIdFilter = matchingProductIds;
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
            if (productIdFilter != null) {
                predicates.add(root.get("entityId").in(productIdFilter));
            }
            if (userId != null) {
                predicates.add(criteriaBuilder.equal(root.get("user").get("id"), userId));
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
