package com.ferreteria.fc.persistence.entity;

import com.fasterxml.jackson.annotation.JsonBackReference;
import jakarta.persistence.*;
import lombok.*;


import java.math.BigDecimal;

@Entity
@Getter // Genera getters
@Setter // Genera setters
@NoArgsConstructor // Requerido por JPA
@AllArgsConstructor //
@Builder
@Table(name = "order_items")
public class OrderItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id")
    private Product product;

    private String productName;

    private Integer quantity;

    private BigDecimal unitPrice;

    private BigDecimal totalPrice;

    @Column(name = "wholesale_applied")
    @Builder.Default
    private Boolean wholesaleApplied = false;

    @Column(name = "pricing_type", length = 20)
    private String pricingType;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id")
    @JsonBackReference
    private Order order;
}
