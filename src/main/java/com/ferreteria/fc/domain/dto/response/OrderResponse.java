package com.ferreteria.fc.domain.dto.response;

import com.ferreteria.fc.domain.enums.OrderStatus;
import com.ferreteria.fc.domain.enums.PaymentMethod;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderResponse {

    private Long id;
    private String orderNumber;
    private LocalDate deliveryDate;
    private String customerName;
    private String phone;
    private String address;
    private String wazeLink;
    private PaymentMethod paymentMethod;
    private OrderStatus status;
    private String notes;
    private BigDecimal totalAmount;
    private UserResponse createdBy;
    private List<OrderItemResponse> items;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
