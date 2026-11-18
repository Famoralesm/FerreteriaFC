package com.ferreteria.fc.domain.service;

import com.ferreteria.fc.domain.dto.request.CreateInventoryCountRequest;
import com.ferreteria.fc.domain.dto.request.InventoryCountLineRequest;
import com.ferreteria.fc.domain.dto.response.InventoryCountResponse;
import com.ferreteria.fc.persistence.entity.User;
import java.util.List;

public interface InventoryCountService {
    InventoryCountResponse crear(CreateInventoryCountRequest request, User usuario);
    InventoryCountResponse iniciar(Long id, User usuario);
    InventoryCountResponse registrarConteo(Long id, InventoryCountLineRequest request, User usuario);
    InventoryCountResponse completar(Long id, User usuario);
    InventoryCountResponse cancelar(Long id, User usuario);
    InventoryCountResponse obtener(Long id, User usuario);
    List<InventoryCountResponse> listar(User usuario);
}
