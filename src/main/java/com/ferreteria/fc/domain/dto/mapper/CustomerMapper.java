package com.ferreteria.fc.domain.dto.mapper;

import com.ferreteria.fc.domain.dto.response.CustomerResponse;
import com.ferreteria.fc.persistence.entity.Customer;
import org.springframework.stereotype.Component;

import java.util.List;
@Component
public class CustomerMapper {

    public CustomerResponse toResponse(Customer customer){

        if (customer == null) {
            return null;
        }

        return CustomerResponse.builder()
                .id(customer.getId())
                .nombre(customer.getNombre())
                .rut(customer.getRut())
                .razonSocial(customer.getRazonSocial())
                .giro(customer.getGiro())
                .direccion(customer.getDireccion())
                .comuna(customer.getComuna())
                .ciudad(customer.getCiudad())
                .email(customer.getEmail())
                .telefono(customer.getTelefono())
                .activo(customer.getActivo())
                .createdAt(customer.getCreatedAt())
                .updatedAt(customer.getUpdatedAt())
                .build();
    }
    public List<CustomerResponse> toListResponse(List<Customer> customers) {

        return customers.stream()
                .map(this::toResponse)
                .toList();
    }

}
