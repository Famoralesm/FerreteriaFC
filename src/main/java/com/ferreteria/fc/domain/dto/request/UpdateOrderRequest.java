package com.ferreteria.fc.domain.dto.request;

import com.ferreteria.fc.domain.enums.OrderStatus;
import com.ferreteria.fc.domain.enums.PaymentMethod;
import lombok.*;

import java.time.LocalDate;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdateOrderRequest {

    private String customerName;


    private String phone;

    private String address;

    private LocalDate deliveryDate;

    private PaymentMethod paymentMethod;

    private OrderStatus status;

    private String notes;

    private List<OrderItemRequest> items;
}