package com.ferreteria.fc.persistence.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(
        name = "purchase_items",
        indexes = {
                @Index(name = "idx_purchase_item_purchase", columnList = "purchase_id"),
                @Index(name = "idx_purchase_item_product", columnList = "product_id")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PurchaseItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "purchase_id", nullable = false)
    private Purchase purchase;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    // Puede ser null cuando la compra se registra directamente en la unidad base.
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_unit_id")
    private ProductUnit productUnit;

    // Datos históricos para que cambios posteriores del catálogo no alteren la compra.
    @Column(name = "product_sku_snapshot", nullable = false, length = 50)
    private String productSkuSnapshot;

    @Column(name = "product_name_snapshot", nullable = false, length = 150)
    private String productNameSnapshot;

    @Column(name = "unit_code_snapshot", nullable = false, length = 30)
    private String unitCodeSnapshot;

    @Column(name = "unit_name_snapshot", nullable = false, length = 80)
    private String unitNameSnapshot;

    @Column(name = "conversion_factor_snapshot", nullable = false, precision = 14, scale = 4)
    private BigDecimal conversionFactorSnapshot;

    // Cantidad expresada en la presentación seleccionada.
    @Column(nullable = false, precision = 14, scale = 4)
    private BigDecimal quantity;

    // Cantidad convertida a la unidad base del inventario.
    @Column(name = "quantity_base_unit", nullable = false, precision = 14, scale = 4)
    private BigDecimal quantityBaseUnit;

    // Precio de compra por unidad de la presentación seleccionada.
    @Column(name = "unit_purchase_price", nullable = false, precision = 15, scale = 2)
    private BigDecimal unitPurchasePrice;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal subtotal;
}
