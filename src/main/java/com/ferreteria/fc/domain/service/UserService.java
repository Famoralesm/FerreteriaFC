package com.ferreteria.fc.domain.service;

import com.ferreteria.fc.domain.dto.request.UserRequest;
import com.ferreteria.fc.domain.dto.response.UserResponse;
import com.ferreteria.fc.persistence.entity.User;

import java.util.List;
import java.util.Optional;

public interface UserService {

    UserResponse create(UserRequest request);

    List<UserResponse> getAll();

    UserResponse getById(Long id);

    UserResponse update(Long id, UserRequest request);

    void delete(Long id);

    User getByEmail(String email);
    Optional<User> getOptionalByEmail(String nombreUsuario);
}