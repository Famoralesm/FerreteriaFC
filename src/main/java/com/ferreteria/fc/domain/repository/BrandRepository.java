package com.ferreteria.fc.domain.repository;

import com.ferreteria.fc.persistence.entity.Brand;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface BrandRepository extends JpaRepository<Brand, Long> {
    List<Brand> findByActiveTrueOrderByNameAsc();
    Optional<Brand> findByIdAndActiveTrue(Long id);
    boolean existsByNameIgnoreCase(String name);
}
