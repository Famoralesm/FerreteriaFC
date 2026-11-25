package com.ferreteria.fc.persistence.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "brands", uniqueConstraints = @UniqueConstraint(name = "uk_brand_name", columnNames = "name"))
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Brand {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false, unique = true, length = 100)
    private String name;
    @Column(nullable = false)
    @Builder.Default
    private Boolean active = true;
}
