package com.ferreteria.fc.web.controller;

import com.ferreteria.fc.config.JwtService;
import com.ferreteria.fc.domain.dto.request.LoginRequest;
import com.ferreteria.fc.domain.dto.response.LoginResponse;
import com.ferreteria.fc.domain.dto.response.UserResponse;
import com.ferreteria.fc.domain.service.UserService;
import com.ferreteria.fc.persistence.entity.User;
import com.ferreteria.fc.domain.dto.mapper.UserMapper;
import com.ferreteria.fc.domain.dto.request.UpdateMyProfileRequest;

import jakarta.validation.Valid;

import org.springframework.http.MediaType;
import org.springframework.web.multipart.MultipartFile;
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

        return UserMapper.toResponse(user);
    }


    @PutMapping("/me/profile")
    public UserResponse updateMyProfile(
            Authentication authentication,
            @Valid @RequestBody UpdateMyProfileRequest request
    ) {

        return userService.updateMyProfile(
                authentication.getName(),
                request
        );

    }


    @PutMapping(
            value = "/me/avatar",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    public UserResponse uploadMyAvatar(
            Authentication authentication,
            @RequestParam("file") MultipartFile file
    ) {

        return userService.uploadMyAvatar(
                authentication.getName(),
                file
        );

    }


    @DeleteMapping("/me/avatar")
    public UserResponse deleteMyAvatar(
            Authentication authentication
    ) {

        return userService.deleteMyAvatar(
                authentication.getName()
        );

    }
}