package com.ferreteria.fc.domain.service;

import com.ferreteria.fc.domain.dto.request.InventoryMovementRequest;
import com.ferreteria.fc.domain.dto.response.InventoryMovementResponse;
import com.ferreteria.fc.persistence.entity.User;

import java.util.List;

public interface InventoryService {

    InventoryMovementResponse registerEntry(
            InventoryMovementRequest request,
            User usuario
    );

    InventoryMovementResponse registerExit(
            InventoryMovementRequest request,
            User usuario
    );

    InventoryMovementResponse registerReturn(
            InventoryMovementRequest request,
            User usuario
    );

    InventoryMovementResponse registerAdjustment(
            InventoryMovementRequest request,
            User usuario
    );

    List<InventoryMovementResponse> getMovements(
            Long productoId,
            Long sucursalId,
            User usuario
    );
}