package com.ferreteria.fc.domain.service;

import com.ferreteria.fc.persistence.entity.Customer;

import java.util.List;

public interface CustomerService {

    public Customer create(Customer customer);

    public List<Customer> getAll();

    public Customer getById(Long id);

    public void delete(Long id) ;
}
