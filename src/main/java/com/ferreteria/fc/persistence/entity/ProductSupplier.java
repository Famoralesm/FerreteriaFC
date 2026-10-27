package com.ferreteria.fc.persistence.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(
        name = "product_supplier",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_product_supplier",
                        columnNames = {"product_id", "supplier_id"}
                )
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductSupplier {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "supplier_id", nullable = false)
    private Supplier supplier;

    @Column(
            name = "purchase_price",
            nullable = false,
            precision = 15,
            scale = 2
    )
    private BigDecimal purchasePrice;

    @Column(nullable = false)
    @Builder.Default
    private Boolean selected = false;
}