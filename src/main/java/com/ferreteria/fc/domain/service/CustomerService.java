package com.ferreteria.fc.domain.service;

import com.ferreteria.fc.domain.dto.request.CreateCustomerRequest;
import com.ferreteria.fc.domain.dto.response.CustomerResponse;
import com.ferreteria.fc.persistence.entity.Customer;

import java.util.List;

public interface CustomerService {

    public CustomerResponse createCustomer(CreateCustomerRequest customer);

    public  List<CustomerResponse> getAll();

    public CustomerResponse getById(Long id);

    public void delete(Long id) ;
}
