package com.ferreteria.fc.domain.service;

import com.ferreteria.fc.domain.dto.request.CreateOrderRequest;
import com.ferreteria.fc.domain.dto.request.UpdateOrderRequest;
import com.ferreteria.fc.domain.dto.request.UpdatePatchOrderRequest;
import com.ferreteria.fc.domain.dto.response.OrderResponse;
import com.ferreteria.fc.persistence.entity.User;

import java.util.List;

public interface OrderService {
    OrderResponse createOrder(CreateOrderRequest request, User user);
    public List<OrderResponse> getAll();
    public OrderResponse getById(Long id);
    public OrderResponse updateOrder(Long id, UpdateOrderRequest order);
    public OrderResponse patchOrder(Long id, UpdatePatchOrderRequest order);
    public void delete(Long id);
}