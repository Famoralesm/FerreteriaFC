package com.ferreteria.fc.domain.service;

import com.ferreteria.fc.dto.purchase.PurchaseRequest;
import com.ferreteria.fc.dto.purchase.PurchaseResponse;
import com.ferreteria.fc.persistence.entity.User;

public interface PurchaseService {

    PurchaseResponse register(PurchaseRequest request, User usuario);
}
