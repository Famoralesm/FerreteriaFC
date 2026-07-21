package com.ferreteria.fc.domain.service;

import com.ferreteria.fc.persistence.entity.User;

import java.util.List;

public interface UserService {
     User create(User user);
     List<User> getAll();
     User getById(Long id);
     void delete(Long id);
     User getByEmail(String email);
}
