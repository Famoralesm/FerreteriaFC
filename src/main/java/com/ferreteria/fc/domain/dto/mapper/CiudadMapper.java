package com.ferreteria.fc.domain.dto.mapper;

import com.ferreteria.fc.domain.dto.response.CiudadResponse;
import com.ferreteria.fc.domain.dto.response.CustomerResponse;
import com.ferreteria.fc.persistence.entity.Ciudad;
import com.ferreteria.fc.persistence.entity.Customer;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class CiudadMapper {
    public CiudadResponse toResponse(Ciudad ciudad){

        if (ciudad == null) {
            return null;
        }

        return CiudadResponse.builder()
                .id(ciudad.getId())
                .nombre(ciudad.getNombre())
                .build();
    }

    public List<CiudadResponse> toListResponse(List<Ciudad> ciudades) {

        return ciudades.stream()
                .map(this::toResponse)
                .toList();
    }

}
