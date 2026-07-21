package com.ferreteria.fc.persistence.entity;

import com.ferreteria.fc.domain.enums.Role;
import jakarta.persistence.*;
import lombok.*;



@Entity
@Getter // Genera getters
@Setter // Genera setters
@NoArgsConstructor
@AllArgsConstructor //
@Builder
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true)
    private String email;

    private String name;

    private String password;

    @Enumerated(EnumType.STRING)
    private Role role;
    @Builder.Default
    private boolean active = true;


}
