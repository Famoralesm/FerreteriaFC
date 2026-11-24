package com.ferreteria.fc.domain.repository;

import com.ferreteria.fc.persistence.entity.UnitOfMeasure;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface UnitOfMeasureRepository extends JpaRepository<UnitOfMeasure, Long> {
    List<UnitOfMeasure> findAllByOrderByNameAsc();
    List<UnitOfMeasure> findByActiveTrueOrderByNameAsc();
    Optional<UnitOfMeasure> findByCode(String code);
    boolean existsByCode(String code);
}