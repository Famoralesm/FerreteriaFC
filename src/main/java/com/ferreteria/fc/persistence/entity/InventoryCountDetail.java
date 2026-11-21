
package com.ferreteria.fc.persistence.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import lombok.*;

@Entity
@Table(
        name = "inventory_count_details",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_inventory_count_product",
                        columnNames = {"inventory_count_id", "product_id"}
                )
        },
        indexes = {
                @Index(name = "idx_inventory_count_detail_product",
                        columnList = "product_id")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InventoryCountDetail {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "inventory_count_id", nullable = false)
    private InventoryCount inventoryCount;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    // Stock que tenía el sistema cuando se agregó al conteo.
    @Column(name = "expected_stock", nullable = false, precision = 14, scale = 4)
    private BigDecimal expectedStock;

    // Cantidad contada físicamente.
    @Column(name = "counted_stock", precision = 14, scale = 4)
    private BigDecimal countedStock;

    // countedStock - expectedStock.
    @Column(name = "difference", precision = 14, scale = 4)
    private BigDecimal difference;

    @Column(length = 500)
    private String notes;

    // Evita aplicar dos veces la misma diferencia.
    @Column(name = "adjustment_applied", nullable = false)
    @Builder.Default
    private Boolean adjustmentApplied = false;
}
