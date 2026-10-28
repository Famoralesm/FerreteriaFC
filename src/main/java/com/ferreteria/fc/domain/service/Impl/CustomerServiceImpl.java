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
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class CustomerServiceImpl implements CustomerService {

    private final CustomerRepository customerRepository;
    private final CustomerMapper customerMapper;
    private final CiudadRepository ciudadRepository;
    private final ComunaRepository comunaRepository;

    public CustomerServiceImpl(
            CustomerRepository customerRepository,
            CustomerMapper customerMapper,
            CiudadRepository ciudadRepository,
            ComunaRepository comunaRepository
    ) {
        this.customerRepository = customerRepository;
        this.customerMapper = customerMapper;
        this.ciudadRepository = ciudadRepository;
        this.comunaRepository = comunaRepository;
    }

    public CustomerResponse createCustomer(CreateCustomerRequest request) {

        String rut = normalizarRut(request.getRut());

        if (customerRepository.existsByRut(rut)) {
            throw new RuntimeException(
                    "Ya existe un cliente con rut: " + rut
            );
        }

        Ciudad ciudad = ciudadRepository.findById(
                request.getCiudad().getId()
        ).orElseThrow(() ->
                new RuntimeException("Ciudad no encontrada")
        );

        Comuna comuna = comunaRepository.findById(
                request.getComuna().getId()
        ).orElseThrow(() ->
                new RuntimeException("Comuna no encontrada")
        );

        Customer customer = Customer.builder()
                .rut(rut)
                .nombre(request.getNombre())
                .razonSocial(request.getRazonSocial())
                .giro(request.getGiro())
                .direccion(request.getDireccion())
                .comuna(comuna)
                .ciudad(ciudad)
                .email(request.getEmail())
                .telefono(request.getTelefono())
                .activo(
                        request.getActivo() != null
                                ? request.getActivo()
                                : true
                )
                .build();

        Customer saved = customerRepository.save(customer);

        return customerMapper.toResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<CustomerResponse> getAll() {
        return customerMapper.toListResponse(
                customerRepository.findAll()
        );
    }

    @Transactional(readOnly = true)
    public CustomerResponse getById(Long id) {

        Customer customer = customerRepository.findById(id)
                .orElseThrow(() ->
                        new RuntimeException("Cliente no encontrado")
                );

        return customerMapper.toResponse(customer);
    }

    public CustomerResponse updateCustomer(
            Long id,
            CreateCustomerRequest request
    ) {
        Customer customer = customerRepository.findById(id)
                .orElseThrow(() ->
                        new RuntimeException("Cliente no encontrado")
                );

        String rut = normalizarRut(request.getRut());

        boolean rutUsadoPorOtroCliente =
                customerRepository.findByRut(rut)
                        .filter(existente ->
                                !existente.getId().equals(id))
                        .isPresent();

        if (rutUsadoPorOtroCliente) {
            throw new RuntimeException(
                    "Ya existe otro cliente con rut: " + rut
            );
        }

        Ciudad ciudad = ciudadRepository.findById(
                request.getCiudad().getId()
        ).orElseThrow(() ->
                new RuntimeException("Ciudad no encontrada")
        );

        Comuna comuna = comunaRepository.findById(
                request.getComuna().getId()
        ).orElseThrow(() ->
                new RuntimeException("Comuna no encontrada")
        );

        customer.setRut(rut);
        customer.setNombre(request.getNombre());
        customer.setRazonSocial(request.getRazonSocial());
        customer.setGiro(request.getGiro());
        customer.setDireccion(request.getDireccion());
        customer.setCiudad(ciudad);
        customer.setComuna(comuna);
        customer.setEmail(request.getEmail());
        customer.setTelefono(request.getTelefono());

        if (request.getActivo() != null) {
            customer.setActivo(request.getActivo());
        }

        Customer updated = customerRepository.save(customer);

        return customerMapper.toResponse(updated);
    }

    public void delete(Long id) {

        if (!customerRepository.existsById(id)) {
            throw new RuntimeException("Cliente no encontrado");
        }

        customerRepository.deleteById(id);
    }

    private String normalizarRut(String rut) {
        return rut == null
                ? ""
                : rut.trim()
                .replace(".", "")
                .replace("-", "")
                .toUpperCase();
    }
}