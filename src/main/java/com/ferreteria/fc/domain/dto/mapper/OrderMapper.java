package com.ferreteria.fc.domain.dto.mapper;

import com.ferreteria.fc.domain.dto.request.UpdateOrderRequest;
import com.ferreteria.fc.domain.dto.response.OrderItemResponse;
import com.ferreteria.fc.domain.dto.response.OrderResponse;
import com.ferreteria.fc.domain.dto.response.UserResponse;
import com.ferreteria.fc.domain.dto.utils.WazeUtils;
import com.ferreteria.fc.persistence.entity.Order;
import com.ferreteria.fc.persistence.entity.OrderItem;
import com.ferreteria.fc.persistence.entity.User;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Component
public class OrderMapper {

    public OrderResponse toResponse(Order order) {

        if(order == null){
            return null;
        }

        return OrderResponse.builder()

                .id(order.getId())
                .orderNumber(order.getOrderNumber())
                .deliveryDate(order.getDeliveryDate())
                .customerName(order.getCustomerName())
                .phone(order.getPhone())
                .address(order.getAddress())
                .wazeLink(order.getWazeLink())
                .paymentMethod(order.getPaymentMethod())
                .status(order.getStatus())
                .notes(order.getNotes())
                .totalAmount(order.getTotalAmount())
                .createdAt(order.getCreatedAt())
                .updatedAt(order.getUpdatedAt())

                .createdBy(toUserResponse(order.getCreatedBy()))

                .items(toItemResponse(order.getItems()))

                .build();

    }
    public List<OrderResponse> toListResponse(List<Order> list){
        List<OrderResponse> resp= new ArrayList<>();
        if (list!=null){
            for (Order order : list) {
                resp.add(this.toResponse(order));
            }
        }
        return resp;
    }
    private UserResponse toUserResponse(User user){

        if(user == null){
            return null;
        }

        return UserResponse.builder()
                .id(user.getId())
                .name(user.getName())
                .email(user.getEmail())
                .build();

    }

    private List<OrderItemResponse> toItemResponse(List<OrderItem> items){

        return items.stream()

                .map(item -> OrderItemResponse.builder()

                        .id(item.getId())
                        .productName(item.getProductName())
                        .quantity(item.getQuantity())
                        .unitPrice(item.getUnitPrice())
                        .totalPrice(item.getTotalPrice())

                        .build())

                .toList();

    }

    public Order toEntity(UpdateOrderRequest request) {

        if(request == null){
            return null;
        }

        Order order = new Order();

        order.setCustomerName(request.getCustomerName());
        order.setPhone(request.getPhone());
        order.setAddress(request.getAddress());
        order.setDeliveryDate(request.getDeliveryDate());
        order.setPaymentMethod(request.getPaymentMethod());
        order.setStatus(request.getStatus());
        order.setNotes(request.getNotes());

        List<OrderItem> items = request.getItems().stream()
                .map(item -> OrderItem.builder()
                        .productName(item.getProductName())
                        .quantity(item.getQuantity())
                        .unitPrice(item.getUnitPrice())
                        .totalPrice(
                                item.getUnitPrice()
                                        .multiply(BigDecimal.valueOf(item.getQuantity()))
                        )
                        .build())
                .toList();

        items.forEach(i -> i.setOrder(order));

        order.setItems(items);

        order.setTotalAmount(
                items.stream()
                        .map(OrderItem::getTotalPrice)
                        .reduce(BigDecimal.ZERO, BigDecimal::add)
        );

        order.setWazeLink(WazeUtils.generateLink(request.getAddress()));

        return order;
    }


}