package com.ferreteria.fc.domain.repository;

import com.ferreteria.fc.persistence.entity.Customer;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CustomerRepository extends JpaRepository<Customer, Long> {
}
