package com.ferreteria.fc.persistence.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;


import java.time.LocalDateTime;

@Entity
@Getter // Genera getters
@Setter // Genera setters
@NoArgsConstructor
@AllArgsConstructor //
public class AuditLog {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String entity;
    private Long entityId;
    private String action;
    private String performedBy;
    private LocalDateTime performedAt;
}