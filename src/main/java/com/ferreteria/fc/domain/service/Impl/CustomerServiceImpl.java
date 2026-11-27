package com.ferreteria.fc.domain.service.Impl;

import com.ferreteria.fc.domain.dto.mapper.CustomerMapper;
import com.ferreteria.fc.domain.dto.request.CreateCustomerRequest;
import com.ferreteria.fc.domain.dto.response.CustomerResponse;
import com.ferreteria.fc.domain.repository.CiudadRepository;
import com.ferreteria.fc.domain.repository.ComunaRepository;
import com.ferreteria.fc.domain.repository.CustomerRepository;
import com.ferreteria.fc.domain.service.CustomerService;
import com.ferreteria.fc.domain.service.AuditLogService;
import com.ferreteria.fc.persistence.entity.Ciudad;
import com.ferreteria.fc.persistence.entity.Comuna;
import com.ferreteria.fc.persistence.entity.Customer;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

@Service
@Transactional
public class CustomerServiceImpl implements CustomerService {

    private final CustomerRepository customerRepository;
    private final CustomerMapper customerMapper;
    private final CiudadRepository ciudadRepository;
    private final ComunaRepository comunaRepository;
    private final AuditLogService auditLogService;

    public CustomerServiceImpl(
            CustomerRepository customerRepository,
            CustomerMapper customerMapper,
            CiudadRepository ciudadRepository,
            ComunaRepository comunaRepository,
            AuditLogService auditLogService
    ) {
        this.customerRepository = customerRepository;
        this.customerMapper = customerMapper;
        this.ciudadRepository = ciudadRepository;
        this.comunaRepository = comunaRepository;
        this.auditLogService = auditLogService;
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
        auditLogService.recordForCurrentUser(
                "CUSTOMERS", "CUSTOMER", saved.getId().toString(), "CREATED", null,
                null, Map.of("active", saved.getActivo(), "nombre", saved.getNombre(), "capturedFields",
                        List.of("rut", "nombre", "razonSocial", "giro", "direccion", "comuna", "ciudad", "email", "telefono")),
                "Cliente creado (datos personales omitidos de la bitácora)", "WEB"
        );

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

        String rutAnterior = customer.getRut();
        String nombreAnterior = customer.getNombre();
        String razonSocialAnterior = customer.getRazonSocial();
        String giroAnterior = customer.getGiro();
        String direccionAnterior = customer.getDireccion();
        String emailAnterior = customer.getEmail();
        String telefonoAnterior = customer.getTelefono();
        Long ciudadAnterior = customer.getCiudad() != null ? customer.getCiudad().getId() : null;
        Long comunaAnterior = customer.getComuna() != null ? customer.getComuna().getId() : null;
        boolean activoAnterior = customer.getActivo();

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
        List<String> changedFields = new ArrayList<>();
        if (!Objects.equals(rutAnterior, updated.getRut())) changedFields.add("rut");
        if (!Objects.equals(nombreAnterior, updated.getNombre())) changedFields.add("nombre");
        if (!Objects.equals(razonSocialAnterior, updated.getRazonSocial())) changedFields.add("razonSocial");
        if (!Objects.equals(giroAnterior, updated.getGiro())) changedFields.add("giro");
        if (!Objects.equals(direccionAnterior, updated.getDireccion())) changedFields.add("direccion");
        if (!Objects.equals(emailAnterior, updated.getEmail())) changedFields.add("email");
        if (!Objects.equals(telefonoAnterior, updated.getTelefono())) changedFields.add("telefono");
        if (!Objects.equals(ciudadAnterior, updated.getCiudad() != null ? updated.getCiudad().getId() : null)) changedFields.add("ciudad");
        if (!Objects.equals(comunaAnterior, updated.getComuna() != null ? updated.getComuna().getId() : null)) changedFields.add("comuna");
        if (activoAnterior != updated.getActivo()) changedFields.add("activo");
        if (!changedFields.isEmpty()) {
            auditLogService.recordForCurrentUser(
                    "CUSTOMERS", "CUSTOMER", updated.getId().toString(),
                    activoAnterior && !updated.getActivo() ? "DEACTIVATED" : !activoAnterior && updated.getActivo() ? "ACTIVATED" : "UPDATED",
                    null,
                    Map.of("changedFields", changedFields, "nombre", nombreAnterior),
                    Map.of("changedFields", changedFields, "nombre", updated.getNombre(), "active", updated.getActivo()),
                    "Cliente actualizado",
                    "WEB"
            );
        }

        return customerMapper.toResponse(updated);
    }

    public void delete(Long id) {

        if (!customerRepository.existsById(id)) {
            throw new RuntimeException("Cliente no encontrado");
        }

        auditLogService.recordForCurrentUser(
                "CUSTOMERS", "CUSTOMER", id.toString(), "DELETED", null,
                Map.of("exists", true), Map.of("deleted", true),
                "Cliente eliminado; los datos personales no se copian a la bitácora", "WEB"
        );
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