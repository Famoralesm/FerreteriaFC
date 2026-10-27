package com.ferreteria.fc.persistence.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "comuna")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Comuna {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;


    @Column(nullable = false, length = 200)
    private String nombre;

    @ManyToOne
    @JoinColumn(name = "ciudad_id", nullable = false)
    private Ciudad ciudad;
}
