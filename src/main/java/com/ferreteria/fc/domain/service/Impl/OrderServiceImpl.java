package com.ferreteria.fc.domain.service.Impl;

import com.ferreteria.fc.domain.dto.mapper.OrderMapper;
import com.ferreteria.fc.domain.dto.request.CreateOrderRequest;
import com.ferreteria.fc.domain.dto.request.UpdateOrderRequest;
import com.ferreteria.fc.domain.dto.request.UpdatePatchOrderRequest;
import com.ferreteria.fc.domain.dto.response.OrderResponse;
import com.ferreteria.fc.domain.dto.response.OrderSummaryResponse;
import com.ferreteria.fc.domain.dto.utils.WazeUtils;
import com.ferreteria.fc.domain.enums.OrderStatus;
import com.ferreteria.fc.domain.repository.OrderRepository;
import com.ferreteria.fc.domain.service.OrderService;
import com.ferreteria.fc.persistence.entity.Order;
import com.ferreteria.fc.persistence.entity.OrderItem;
import com.ferreteria.fc.persistence.entity.User;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
public class OrderServiceImpl implements OrderService {

    private final OrderRepository orderRepository;
    private final OrderMapper orderMapper;

    public OrderServiceImpl(OrderRepository orderRepository, OrderMapper orderMapper) {
        this.orderRepository = orderRepository;
        this.orderMapper = orderMapper;
    }

    @Override
    @Transactional
    public OrderResponse createOrder(CreateOrderRequest request, User user) {

        Order order = new Order();
        order.setOrderNumber(UUID.randomUUID().toString());
        order.setCustomerName(request.getCustomerName());
        order.setPhone(request.getPhone());
        order.setAddress(request.getAddress());
        order.setPaymentMethod(request.getPaymentMethod());
        order.setStatus(OrderStatus.CREATED);
        order.setStatusChangedAt(LocalDateTime.now());
        order.setNotes(request.getNotes());
        order.setCreatedBy(user);
        order.setDeliveryDate(request.getDeliveryDate());


        List<OrderItem> items = request.getItems().stream().map(i -> {
            OrderItem item = new OrderItem();
            item.setProductName(i.getProductName());
            item.setQuantity(i.getQuantity());
            item.setUnitPrice(i.getUnitPrice());
            item.setTotalPrice(
                    i.getUnitPrice().multiply(BigDecimal.valueOf(i.getQuantity()))
            );
            item.setOrder(order);
            return item;
        }).toList();

        order.setItems(items);
        order.setTotalAmount(
                items.stream()
                        .map(OrderItem::getTotalPrice)
                        .reduce(BigDecimal.ZERO, BigDecimal::add)
        );

        order.setWazeLink(WazeUtils.generateLink(order.getAddress()));

         orderRepository.save(order);
        return orderMapper.toResponse(order);
    }
    public List<OrderResponse> getAll() {
        return orderMapper.toListResponse(orderRepository.findAll());
    }

    public OrderResponse getById(Long id) {
        return orderMapper.toResponse(orderRepository.getReferenceById(id));
    }


    @Override
    @Transactional
    public OrderResponse updateOrder(Long id, UpdateOrderRequest request) {

        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Orden no encontrada"));

        order.setCustomerName(request.getCustomerName());
        order.setPhone(request.getPhone());
        order.setAddress(request.getAddress());
        order.setDeliveryDate(request.getDeliveryDate());
        order.setPaymentMethod(request.getPaymentMethod());
        if (request.getStatus() != null &&
                request.getStatus() != order.getStatus()) {

            order.setStatus(request.getStatus());
            order.setStatusChangedAt(LocalDateTime.now());
        }
        order.setNotes(request.getNotes());

        order.setWazeLink(WazeUtils.generateLink(request.getAddress()));

        if (request.getItems() != null) {

            order.getItems().clear();

            List<OrderItem> items = request.getItems().stream()
                    .map(item -> {

                        OrderItem orderItem = new OrderItem();

                        orderItem.setProductName(item.getProductName());
                        orderItem.setQuantity(item.getQuantity());
                        orderItem.setUnitPrice(item.getUnitPrice());

                        orderItem.setTotalPrice(
                                item.getUnitPrice()
                                        .multiply(
                                                BigDecimal.valueOf(item.getQuantity())
                                        )
                        );

                        orderItem.setOrder(order);

                        return orderItem;

                    })
                    .toList();

            order.getItems().addAll(items);

            order.setTotalAmount(
                    items.stream()
                            .map(OrderItem::getTotalPrice)
                            .reduce(BigDecimal.ZERO, BigDecimal::add)
            );
        }

        return orderMapper.toResponse(orderRepository.save(order));
    }

    @Transactional
    public OrderResponse patchOrder(Long id, UpdatePatchOrderRequest request) {

        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Pedido no encontrado"));

        if(request.getDeliveryDate() != null){
            order.setDeliveryDate(request.getDeliveryDate());
        }

        if(request.getCustomerName() != null){
            order.setCustomerName(request.getCustomerName());
        }

        if(request.getPhone() != null){
            order.setPhone(request.getPhone());
        }

        if(request.getAddress() != null){
            order.setAddress(request.getAddress());
            order.setWazeLink(WazeUtils.generateLink(request.getAddress()));
        }

        if(request.getPaymentMethod() != null){
            order.setPaymentMethod(request.getPaymentMethod());
        }

        if(request.getNotes() != null){
            order.setNotes(request.getNotes());
        }

        if (request.getStatus() != null &&
                request.getStatus() != order.getStatus()) {

            order.setStatus(request.getStatus());
            order.setStatusChangedAt(LocalDateTime.now());
        }

        Order saved = orderRepository.save(order);

        return orderMapper.toResponse(saved);
    }

    public void delete(Long id) {
        orderRepository.deleteById(id);
    }

    @Override
    public List<OrderSummaryResponse> findLatestOrders() {
        return orderRepository.findLatestOrders(PageRequest.of(0,10));
    }

    @Override
    public List<OrderSummaryResponse> findAllSummary() {
        return orderRepository.findAllOrders();
    }


}
