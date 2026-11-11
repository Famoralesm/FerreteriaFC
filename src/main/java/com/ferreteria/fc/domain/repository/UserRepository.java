package com.ferreteria.fc.domain.repository;

import com.ferreteria.fc.persistence.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findOptionalByEmail(String email);
    User findByEmail(String email);
    boolean existsByEmail(String email);

    boolean existsByEmailAndIdNot(String email, Long id);

    long countByRoleAndActiveTrue(
            com.ferreteria.fc.domain.enums.Role role
    );
    long countByAvatarPath(String avatarPath);
}