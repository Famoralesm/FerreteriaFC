package com.ferreteria.fc.domain.repository;

import com.ferreteria.fc.persistence.entity.Comuna;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ComunaRepository extends JpaRepository<Comuna, Long> {
}
