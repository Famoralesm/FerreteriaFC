package com.ferreteria.fc.web.controller;

import com.ferreteria.fc.config.JwtService;
import com.ferreteria.fc.domain.dto.request.LoginRequest;
import com.ferreteria.fc.domain.dto.response.LoginResponse;
import com.ferreteria.fc.domain.dto.response.UserResponse;
import com.ferreteria.fc.domain.service.UserService;
import com.ferreteria.fc.persistence.entity.User;

import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final UserService userService;

    public AuthController(
            AuthenticationManager authenticationManager,
            JwtService jwtService,
            UserService userService) {

        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
        this.userService = userService;
    }

    @PostMapping("/login")
    public LoginResponse login(@RequestBody LoginRequest request) {

        Authentication authentication =
                authenticationManager.authenticate(
                        new UsernamePasswordAuthenticationToken(
                                request.getEmail(),
                                request.getPassword()
                        )
                );

        String token = jwtService.generateToken(authentication);

        return LoginResponse.builder()
                .accessToken(token)
                .tokenType("Bearer")
                .build();
    }

    @GetMapping("/me")
    public UserResponse currentUser(Authentication authentication) {

        User user = userService.getByEmail(authentication.getName());

        if (user == null) {
            throw new UsernameNotFoundException(
                    "Usuario autenticado no encontrado"
            );
        }

        return UserResponse.builder()
                .id(user.getId())
                .name(user.getName())
                .email(user.getEmail())
                .role(user.getRole().name())
                .active(user.isActive())
                .build();
    }
}