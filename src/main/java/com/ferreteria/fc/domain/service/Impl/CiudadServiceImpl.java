package com.ferreteria.fc.domain.service.Impl;

import com.ferreteria.fc.domain.dto.mapper.CiudadMapper;
import com.ferreteria.fc.domain.dto.response.CiudadResponse;
import com.ferreteria.fc.domain.repository.CiudadRepository;
import com.ferreteria.fc.domain.service.CiudadService;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CiudadServiceImpl implements CiudadService {

    private final CiudadRepository ciudadRepository;
    private final CiudadMapper ciudadMapper;

    public CiudadServiceImpl(
            CiudadRepository ciudadRepository,
            CiudadMapper ciudadMapper
    ) {
        this.ciudadRepository = ciudadRepository;
        this.ciudadMapper = ciudadMapper;
    }

    @Override
    public List<CiudadResponse> getAll() {
        return ciudadMapper.toListResponse(
                ciudadRepository.findAll()
        );
    }

    @Override
    public CiudadResponse getById(Long id) {
        return ciudadMapper.toResponse(
                ciudadRepository.findById(id)
                        .orElseThrow(() ->
                                new RuntimeException("Ciudad no encontrada")
                        )
        );
    }
}