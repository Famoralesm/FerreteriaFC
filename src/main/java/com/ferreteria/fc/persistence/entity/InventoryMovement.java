
package com.ferreteria.fc.persistence.entity;

import com.ferreteria.fc.domain.enums.MovementType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.math.BigDecimal;

@Entity
@Table(
        name = "inventory_movements",
        indexes = {
                @Index(name = "idx_inventory_movement_product", columnList = "product_id"),
                @Index(name = "idx_inventory_movement_created", columnList = "created_at"),
                @Index(name = "idx_inventory_movement_reference",
                        columnList = "reference_type, reference_id")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InventoryMovement {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id", nullable = false)
    private Product producto;

    @Enumerated(EnumType.STRING)
    @Column(name = "movement_type", nullable = false, length = 20)
    private MovementType tipoMovimiento;

    // La base de datos conserva el nombre histórico "quantity".
    @Column(name = "quantity", nullable = false, precision = 14, scale = 4)
    private BigDecimal cantidad;

    @Column(name = "stock_before", nullable = false, precision = 14, scale = 4)
    private BigDecimal stockAnterior;

    @Column(name = "stock_after", nullable = false, precision = 14, scale = 4)
    private BigDecimal stockNuevo;

    @Column(length = 500)
    private String motivo;

    @Column(name = "reference_type", length = 40)
    private String tipoReferencia;

    @Column(name = "reference_id")
    private Long referenciaId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by")
    private User usuario;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime fechaCreacion;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "store_id", nullable = false)
    private Store sucursal;
}
