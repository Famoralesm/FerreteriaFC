package com.ferreteria.fc.persistence.entity;

import com.fasterxml.jackson.annotation.JsonBackReference;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
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

    @Column(precision = 14, scale = 4)
    private BigDecimal quantity;

    @Column(name = "unit_id_snapshot")
    private Long unitIdSnapshot;

    @Column(name = "unit_code_snapshot", length = 30)
    private String unitCodeSnapshot;

    @Column(name = "unit_name_snapshot", length = 80)
    private String unitNameSnapshot;

    @Column(name = "conversion_factor_snapshot", precision = 14, scale = 4)
    private BigDecimal conversionFactorSnapshot;

    private BigDecimal unitPrice;

    private BigDecimal totalPrice;

    @Builder.Default
    @Column(name = "wholesale_applied", nullable = false)
    private boolean wholesaleApplied = false;

    @Column(name = "pricing_type", length = 20)
    private String pricingType;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id")
    @JsonBackReference
    private Order order;
}
