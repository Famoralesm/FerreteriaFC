package com.ferreteria.fc.domain.dto.request;

import com.ferreteria.fc.domain.enums.OrderStatus;
import com.ferreteria.fc.domain.enums.PaymentMethod;
import lombok.*;
import jakarta.validation.constraints.NotBlank;

import java.time.LocalDate;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdateOrderRequest {

    @NotBlank
    private String customerName;


    private String phone;

    private String address;

    private String city;

    private String commune;

    private Boolean unknownCustomer;

    private LocalDate deliveryDate;

    private PaymentMethod paymentMethod;

    private OrderStatus status;

    private String notes;

    private List<OrderItemRequest> items;
}