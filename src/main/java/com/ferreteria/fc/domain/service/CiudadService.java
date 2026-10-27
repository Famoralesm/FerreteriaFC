package com.ferreteria.fc.domain.service;

import com.ferreteria.fc.domain.dto.response.CiudadResponse;

import java.util.List;

public interface CiudadService {
    public List<CiudadResponse> getAll();

    public CiudadResponse getById(Long id);
}
