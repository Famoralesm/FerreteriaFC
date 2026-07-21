package com.ferreteria.fc.domain.service;

import com.ferreteria.fc.domain.dto.request.LoginRequest;
import com.ferreteria.fc.domain.dto.response.LoginResponse;

public interface AuthenticationService {

    LoginResponse login(LoginRequest request);

}