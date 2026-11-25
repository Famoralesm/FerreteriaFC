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
                        columnNames = {"product_id", "supplier_id", "brand_id", "product_unit_id"}
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

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "brand_id")
    private Brand brand;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_unit_id")
    private ProductUnit productUnit;

    @Column(name = "conversion_factor_snapshot", nullable = false, precision = 14, scale = 4)
    private BigDecimal conversionFactorSnapshot;

    @Column(name = "unit_code_snapshot", length = 30)
    private String unitCodeSnapshot;

    @Column(name = "unit_name_snapshot", length = 80)
    private String unitNameSnapshot;

    @Column(
            name = "purchase_price",
            nullable = false,
            precision = 15,
            scale = 2
    )
    private BigDecimal purchasePrice;

    @Builder.Default
    @Column(nullable = false)
    private Boolean selected = false;
}