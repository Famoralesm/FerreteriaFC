
package com.ferreteria.fc.persistence.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(
        name = "product_units",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_product_unit_code",
                        columnNames = {"product_id", "code"}
                )
        },
        indexes = {
                @Index(
                        name = "idx_product_unit_product",
                        columnList = "product_id"
                )
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductUnit {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @Column(nullable = false, length = 30)
    private String code;

    @Column(nullable = false, length = 80)
    private String name;

    @Column(
            name = "conversion_factor",
            nullable = false,
            precision = 14,
            scale = 4
    )
    private BigDecimal conversionFactor;

    @Column(name = "purchase_allowed", nullable = false)
    @Builder.Default
    private Boolean purchaseAllowed = true;

    @Column(name = "sale_allowed", nullable = false)
    @Builder.Default
    private Boolean saleAllowed = true;

    @Column(nullable = false)
    @Builder.Default
    private Boolean active = true;
}
