package com.ferreteria.fc.domain.service.Impl;

import com.ferreteria.fc.domain.dto.mapper.CustomerMapper;
import com.ferreteria.fc.domain.dto.request.CreateCustomerRequest;
import com.ferreteria.fc.domain.dto.response.CustomerResponse;
import com.ferreteria.fc.domain.repository.CiudadRepository;
import com.ferreteria.fc.domain.repository.ComunaRepository;
import com.ferreteria.fc.domain.repository.CustomerRepository;
import com.ferreteria.fc.domain.service.CustomerService;
import com.ferreteria.fc.persistence.entity.Ciudad;
import com.ferreteria.fc.persistence.entity.Comuna;
import com.ferreteria.fc.persistence.entity.Customer;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CustomerServiceImpl implements CustomerService {

    private final CustomerRepository customerRepository;
    private final CustomerMapper customerMapper;
    private final CiudadRepository ciudadRepository;
    private final ComunaRepository comunaRepository;

    public CustomerServiceImpl(
            CustomerRepository customerRepository,
            CustomerMapper customerMapper, CiudadRepository ciudadRepository,ComunaRepository comunaRepository
    ) {
        this.customerRepository = customerRepository;
        this.customerMapper=customerMapper;
        this.ciudadRepository=ciudadRepository;
        this.comunaRepository=comunaRepository;
    }

    public CustomerResponse createCustomer(CreateCustomerRequest request) {

        if (customerRepository.existsByRut(request.getRut())) {
            throw new RuntimeException(
                    "Ya existe un cliente con rut: "
                            + request.getRut()
            );
        }
        Ciudad ciudad = ciudadRepository.findById(request.getCiudad().getId())
                .orElseThrow(() ->
                        new RuntimeException("Ciudad no encontrada")
                );

        Comuna comuna = comunaRepository.findById(request.getComuna().getId())
                .orElseThrow(() ->
                        new RuntimeException("Comuna no encontrada")
                );

        Customer customer = Customer.builder()
                .rut(request.getRut())
                .nombre(request.getNombre())
                .razonSocial(request.getRazonSocial())
                .giro(request.getGiro())
                .activo(
                        request.getActivo() != null
                                ? request.getActivo()
                                : true
                )
                .comuna(comuna)
                .ciudad(ciudad)
                .email(request.getEmail())
                .telefono(request.getTelefono())
                .build();


        Customer saved = customerRepository.save(customer);

        return customerMapper.toResponse(saved);
    }

    public List<CustomerResponse> getAll() {
        return customerMapper.toListResponse(customerRepository.findAll());
    }

    public CustomerResponse getById(Long id) {
        Customer resp=customerRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Cliente no encontrado"));
        return customerMapper.toResponse(resp);
    }

    public void delete(Long id) {
        customerRepository.deleteById(id);
    }
}
