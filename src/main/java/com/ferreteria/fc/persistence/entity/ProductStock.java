
package com.ferreteria.fc.persistence.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "product_stocks",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_product_stock_store",
                        columnNames = {"product_id", "store_id"}
                )
        },
        indexes = {
                @Index(name = "idx_product_stock_store", columnList = "store_id"),
                @Index(name = "idx_product_stock_product", columnList = "product_id")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductStock {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id", nullable = false)
    private Product producto;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "store_id", nullable = false)
    private Store sucursal;

    @Column(nullable = false)
    @Builder.Default
    private Integer stock = 0;

    @Column(name = "minimum_stock", nullable = false)
    @Builder.Default
    private Integer stockMinimo = 0;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime fechaActualizacion;
}
