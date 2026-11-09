package com.ferreteria.fc.domain.service.Impl;

import com.ferreteria.fc.domain.dto.mapper.UserMapper;
import com.ferreteria.fc.domain.dto.request.UserRequest;
import com.ferreteria.fc.domain.dto.response.UserResponse;
import com.ferreteria.fc.domain.enums.Role;
import com.ferreteria.fc.domain.repository.UserRepository;
import com.ferreteria.fc.domain.service.UserService;
import com.ferreteria.fc.persistence.entity.User;

import jakarta.transaction.Transactional;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserServiceImpl(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder
    ) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public UserResponse create(UserRequest request) {

        String email = request.getEmail().trim().toLowerCase();

        if (userRepository.existsByEmail(email)) {
            throw new IllegalArgumentException(
                    "Ya existe un usuario registrado con ese correo"
            );
        }

        if (request.getPassword() == null ||
                request.getPassword().isBlank()) {
            throw new IllegalArgumentException(
                    "La contraseña es obligatoria"
            );
        }

        User user = User.builder()
                .name(request.getName().trim())
                .email(email)
                .password(passwordEncoder.encode(request.getPassword()))
                .role(request.getRole())
                .active(request.getActive() == null ||
                        request.getActive())
                .build();

        return UserMapper.toResponse(userRepository.save(user));
    }

    @Override
    public List<UserResponse> getAll() {

        return userRepository.findAll()
                .stream()
                .map(UserMapper::toResponse)
                .toList();
    }

    @Override
    public UserResponse getById(Long id) {

        return UserMapper.toResponse(findUser(id));
    }

    @Override
    public UserResponse update(Long id, UserRequest request) {

        User user = findUser(id);

        String email = request.getEmail().trim().toLowerCase();

        if (userRepository.existsByEmailAndIdNot(email, id)) {
            throw new IllegalArgumentException(
                    "El correo ya está siendo utilizado por otro usuario"
            );
        }

        // No permitimos desactivar al último administrador activo.
        Role newRole = request.getRole() != null
                ? request.getRole()
                : user.getRole();

        boolean newActive = request.getActive() != null
                ? request.getActive()
                : user.isActive();

        boolean isRemovingActiveAdmin =
                user.getRole() == Role.ADMIN &&
                        user.isActive() &&
                        (newRole != Role.ADMIN || !newActive);

        if (isRemovingActiveAdmin) {

            long administrators =
                    userRepository.countByRoleAndActiveTrue(Role.ADMIN);

            if (administrators <= 1) {
                throw new IllegalStateException(
                        "No puedes desactivar o cambiar el perfil del último administrador activo"
                );
            }
        }




        user.setName(request.getName().trim());
        user.setEmail(email);
        user.setRole(request.getRole());
        user.setActive(newActive);


        if (request.getPassword() != null &&
                !request.getPassword().isBlank()) {

            user.setPassword(
                    passwordEncoder.encode(request.getPassword())
            );
        }

        return UserMapper.toResponse(userRepository.save(user));
    }

    @Override
    public void delete(Long id) {

        User user = findUser(id);

        if (user.getRole() == Role.ADMIN && user.isActive()) {

            long administrators =
                    userRepository.countByRoleAndActiveTrue(Role.ADMIN);

            if (administrators <= 1) {
                throw new IllegalStateException(
                        "No puedes eliminar al último administrador activo"
                );
            }
        }

        userRepository.delete(user);
    }

    private User findUser(Long id) {

        return userRepository.findById(id)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Usuario no encontrado: " + id
                        )
                );
    }

    public User getByEmail(String email){
        return userRepository.findByEmail(email);
    }
    public Optional<User> getOptionalByEmail(String nombreUsuario){
        return userRepository.findOptionalByEmail(nombreUsuario);
    }
}