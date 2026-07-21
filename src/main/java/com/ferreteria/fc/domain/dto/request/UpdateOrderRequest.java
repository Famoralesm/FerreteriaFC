package com.ferreteria.fc.domain.dto.request;

import com.ferreteria.fc.domain.enums.OrderStatus;
import com.ferreteria.fc.domain.enums.PaymentMethod;
import jakarta.validation.constraints.NotBlank;
import lombok.*;

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

    @NotBlank
    private String phone;

    @NotBlank
    private String address;

    private LocalDate deliveryDate;

    private PaymentMethod paymentMethod;

    private OrderStatus status;

    private String notes;

    private List<OrderItemRequest> items;
}