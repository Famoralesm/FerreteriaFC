package com.ferreteria.fc.domain.service;

import com.ferreteria.fc.domain.dto.request.CreateCustomerRequest;
import com.ferreteria.fc.domain.dto.response.CustomerResponse;
import com.ferreteria.fc.persistence.entity.Customer;

import java.util.List;

public interface CustomerService {

    CustomerResponse createCustomer(CreateCustomerRequest customer);

    List<CustomerResponse> getAll();

    CustomerResponse getById(Long id);

    void delete(Long id) ;

    CustomerResponse updateCustomer(
            Long id,
            CreateCustomerRequest request
    );
}
