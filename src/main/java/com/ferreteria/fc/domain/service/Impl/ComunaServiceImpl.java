package com.ferreteria.fc.domain.service.Impl;

import com.ferreteria.fc.domain.dto.mapper.ComunaMapper;
import com.ferreteria.fc.domain.dto.response.ComunaResponse;
import com.ferreteria.fc.domain.repository.ComunaRepository;
import com.ferreteria.fc.domain.service.ComunaService;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ComunaServiceImpl implements ComunaService {

    private final ComunaRepository comunaRepository;
    private final ComunaMapper comunaMapper;

    public ComunaServiceImpl(
            ComunaRepository comunaRepository,
            ComunaMapper comunaMapper
    ) {
        this.comunaRepository = comunaRepository;
        this.comunaMapper = comunaMapper;
    }

    @Override
    public List<ComunaResponse> getAll() {
        return comunaMapper.toListResponse(
                comunaRepository.findAll()
        );
    }

    @Override
    public ComunaResponse getById(Long id) {
        return comunaMapper.toResponse(
                comunaRepository.findById(id)
                        .orElseThrow(() ->
                                new RuntimeException("Comuna no encontrada")
                        )
        );
    }
}