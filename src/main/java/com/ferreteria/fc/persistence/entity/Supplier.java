package com.ferreteria.fc.persistence.entity;

import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "supplier")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Supplier {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String nombre;

    private String telefono;

    private String email;

    @OneToMany(
            mappedBy = "supplier",
            fetch = FetchType.LAZY
    )
    @Builder.Default
    private List<ProductSupplier> products = new ArrayList<>();
}