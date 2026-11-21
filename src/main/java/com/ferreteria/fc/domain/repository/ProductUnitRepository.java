
package com.ferreteria.fc.domain.repository;

import com.ferreteria.fc.persistence.entity.ProductUnit;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ProductUnitRepository
        extends JpaRepository<ProductUnit, Long> {

    List<ProductUnit> findByProductIdAndActiveTrueOrderByNameAsc(
            Long productId
    );

    List<ProductUnit> findByProductIdOrderByNameAsc(Long productId);

    Optional<ProductUnit> findByIdAndProductId(
            Long id,
            Long productId
    );

    boolean existsByProductIdAndCode(
            Long productId,
            String code
    );
}
