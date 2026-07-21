package com.ferreteria.fc.domain.dto.request;

import com.ferreteria.fc.domain.enums.OrderStatus;
import com.ferreteria.fc.domain.enums.PaymentMethod;
import lombok.*;

import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdatePatchOrderRequest {
    private LocalDate deliveryDate;

    private String customerName;

    private String phone;

    private String address;

    private PaymentMethod paymentMethod;

    private String notes;

    private OrderStatus status;
}
