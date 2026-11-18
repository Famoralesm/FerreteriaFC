
package com.ferreteria.fc.domain.repository;

import com.ferreteria.fc.persistence.entity.Store;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface StoreRepository extends JpaRepository<Store, Long> {

    Optional<Store> findByCodigo(String codigo);

    boolean existsByCodigo(String codigo);
    List<Store> findByActivaTrueOrderByNombreAsc();
}
