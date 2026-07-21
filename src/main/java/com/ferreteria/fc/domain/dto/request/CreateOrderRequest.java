package com.ferreteria.fc.domain.dto.request;

import com.ferreteria.fc.domain.enums.PaymentMethod;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.util.List;
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CreateOrderRequest {

    @NotNull
    private LocalDate deliveryDate;
    @NotBlank
    private String customerName;
    @NotBlank
    private String phone;
    @NotBlank
    private String address;
    @NotNull
    private PaymentMethod paymentMethod;
    private String notes;
    @NotEmpty
    private List<OrderItemRequest> items;
}