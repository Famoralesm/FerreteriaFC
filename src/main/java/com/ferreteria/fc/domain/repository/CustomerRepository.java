package com.ferreteria.fc.domain.repository;

import com.ferreteria.fc.persistence.entity.Customer;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CustomerRepository
        extends JpaRepository<Customer, Long> {

    Optional<Customer> findByRut(String rut);

    boolean existsByRut(String rut);

}