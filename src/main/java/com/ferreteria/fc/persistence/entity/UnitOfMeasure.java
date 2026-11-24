package com.ferreteria.fc.persistence.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "units_of_measure", uniqueConstraints = @UniqueConstraint(name = "uk_unit_measure_code", columnNames = "code"))
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class UnitOfMeasure {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 30)
    private String code;

    @Column(nullable = false, length = 80)
    private String name;

    @Column(nullable = false, length = 12)
    private String symbol;

    @Column(nullable = false)
    @Builder.Default
    private Boolean active = true;
}