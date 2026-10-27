package com.ferreteria.fc.domain.dto.mapper;

import com.ferreteria.fc.domain.dto.response.CiudadResponse;
import com.ferreteria.fc.domain.dto.response.ComunaResponse;
import com.ferreteria.fc.persistence.entity.Ciudad;
import com.ferreteria.fc.persistence.entity.Comuna;
import org.springframework.stereotype.Component;

import java.util.List;
@Component
public class ComunaMapper {
    public ComunaResponse toResponse(Comuna comuna){

        if (comuna == null) {
            return null;
        }

        return ComunaResponse.builder()
                .id(comuna.getId())
                .nombre(comuna.getNombre())
                .build();
    }
    public List<ComunaResponse> toListResponse(List<Comuna> comunas) {

        return comunas.stream()
                .map(this::toResponse)
                .toList();
    }
}
