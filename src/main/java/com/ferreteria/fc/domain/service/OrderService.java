package com.ferreteria.fc.domain.service;

import com.ferreteria.fc.domain.dto.request.CreateOrderRequest;
import com.ferreteria.fc.domain.dto.request.UpdateOrderRequest;
import com.ferreteria.fc.domain.dto.request.UpdatePatchOrderRequest;
import com.ferreteria.fc.domain.dto.response.OrderResponse;
import com.ferreteria.fc.domain.dto.response.OrderSummaryResponse;
import com.ferreteria.fc.persistence.entity.User;

import java.util.List;

public interface OrderService {
    OrderResponse createOrder(CreateOrderRequest request, User user);
    List<OrderResponse> getAll();
    OrderResponse getById(Long id);
    OrderResponse updateOrder(Long id, UpdateOrderRequest order);
    OrderResponse patchOrder(Long id, UpdatePatchOrderRequest order);
    void delete(Long id);
    List<OrderSummaryResponse> findLatestOrders();
    List<OrderSummaryResponse> findAllSummary();
}