package com.ferreteria.fc.domain.service;

import com.ferreteria.fc.dto.purchase.PurchaseRequest;
import com.ferreteria.fc.dto.purchase.PurchaseResponse;
import com.ferreteria.fc.persistence.entity.User;
import java.util.List;

public interface PurchaseService {

    List<PurchaseResponse> getAll(User usuario);

    PurchaseResponse getById(Long purchaseId, User usuario);

    PurchaseResponse register(PurchaseRequest request, User usuario);

    PurchaseResponse receive(Long purchaseId, User usuario);
}
