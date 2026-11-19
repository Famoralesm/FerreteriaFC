package com.ferreteria.fc.domain.repository;

import com.ferreteria.fc.persistence.entity.Supplier;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.EntityGraph;
import java.util.Optional;

import java.util.List;

public interface SupplierRepository
        extends JpaRepository<Supplier, Long> {

    @EntityGraph(attributePaths = {"comuna", "ciudad"})
    List<Supplier> findAllByOrderByNombreAsc();

    @EntityGraph(attributePaths = {"comuna", "ciudad"})
    Optional<Supplier> findWithLocationById(Long id);
    boolean existsByRut(String rut);
    java.util.Optional<Supplier> findByRut(String rut);
}
