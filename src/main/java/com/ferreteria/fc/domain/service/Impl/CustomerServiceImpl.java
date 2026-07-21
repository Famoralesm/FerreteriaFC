package com.ferreteria.fc.domain.service.Impl;

import com.ferreteria.fc.domain.repository.CustomerRepository;
import com.ferreteria.fc.domain.service.CustomerService;
import com.ferreteria.fc.persistence.entity.Customer;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CustomerServiceImpl implements CustomerService {

    private final CustomerRepository customerRepository;

    public CustomerServiceImpl(CustomerRepository customerRepository) {
        this.customerRepository = customerRepository;
    }

    public Customer create(Customer customer) {
        return customerRepository.save(customer);
    }

    public List<Customer> getAll() {
        return customerRepository.findAll();
    }

    public Customer getById(Long id) {
        return customerRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Cliente no encontrado"));
    }

    public void delete(Long id) {
        customerRepository.deleteById(id);
    }
}
