package com.ferreteria.fc.domain.service;

import com.ferreteria.fc.domain.dto.response.ComunaResponse;

import java.util.List;

public interface ComunaService {
    public List<ComunaResponse> getAll();

    public ComunaResponse getById(Long id);
}
