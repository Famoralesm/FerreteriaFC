package com.ferreteria.fc.domain.repository;

import com.ferreteria.fc.domain.enums.Role;
import com.ferreteria.fc.persistence.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    @EntityGraph(attributePaths = "sucursal")
    @Query("select u from User u")
    List<User> findAllWithSucursal();

    @EntityGraph(attributePaths = "sucursal")
    @Query("select u from User u where u.id = :id")
    Optional<User> findWithSucursalById(@Param("id") Long id);
    Optional<User> findOptionalByEmail(String email);
    User findByEmail(String email);
    boolean existsByEmail(String email);
    boolean existsByEmailAndIdNot(String email, Long id);
    List<User> findByNameContainingIgnoreCase(String name);
    List<User> findByRole(Role role);
    long countByRoleAndActiveTrue(Role role);
    long countByAvatarPath(String avatarPath);
}