package com.ferreteria.fc.persistence.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "customers",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_customer_rut",
                        columnNames = "rut"
                )
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Customer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Identificación tributaria
    @Column(nullable = false, unique = true, length = 12)
    private String rut;

    // Persona / empresa
    @Column(nullable = false, length = 200)
    private String nombre;

    @Column(length = 200)
    private String razonSocial;

    @Column(length = 150)
    private String giro;

    // Ubicación
    @Column(length = 250)
    private String direccion;

    @ManyToOne
    @JoinColumn(name = "comuna_id")
    private Comuna comuna;

    @ManyToOne
    @JoinColumn(name = "ciudad_id")
    private Ciudad ciudad;

    @Column(length = 150)
    private String email;

    @Column(length = 30)
    private String telefono;

    @Column(nullable = false)
    private Boolean activo = true;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {

        LocalDateTime now = LocalDateTime.now();

        createdAt = now;
        updatedAt = now;

    }

    @PreUpdate
    protected void onUpdate() {

        updatedAt = LocalDateTime.now();

    }

}