package com.ferreteria.fc.persistence.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(
        name = "supplier",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_supplier_rut",
                        columnNames = "rut"
                )
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Supplier {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(length = 12)
    private String rut;

    @Column(nullable = false, length = 200)
    private String nombre;

    @Column(length = 200)
    private String razonSocial;

    @Column(length = 150)
    private String giro;

    @Column(length = 250)
    private String direccion;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "comuna_id")
    private Comuna comuna;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ciudad_id")
    private Ciudad ciudad;

    @Column(length = 30)
    private String telefono;

    @Column(length = 150)
    private String email;

    @Builder.Default
    @Column(nullable = false)
    private Boolean activo = true;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @OneToMany(mappedBy = "supplier", fetch = FetchType.LAZY)
    @Builder.Default
    private List<ProductSupplier> products = new ArrayList<>();
}