package com.ferreteria.fc.domain.service;

import com.ferreteria.fc.domain.dto.request.CreateCommercialInvoiceRequest;
import com.ferreteria.fc.domain.dto.response.CommercialInvoiceResponse;
import com.ferreteria.fc.persistence.entity.User;
import java.util.List;

public interface CommercialInvoiceService {
    CommercialInvoiceResponse create(CreateCommercialInvoiceRequest request, User user);
    CommercialInvoiceResponse createFromOrder(Long orderId, User user);
    List<CommercialInvoiceResponse> findAll(Long storeId, User user);
    CommercialInvoiceResponse findById(Long id, User user);
    CommercialInvoiceResponse cancel(Long id, User user);
}
