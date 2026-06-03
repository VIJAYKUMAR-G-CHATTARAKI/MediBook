package com.medibook.medibookservice.service;

import com.medibook.medibookservice.dto.request.LoginRequest;
import com.medibook.medibookservice.dto.request.RegisterRequest;
import com.medibook.medibookservice.dto.response.AuthResponse;
import com.medibook.medibookservice.dto.response.UserResponse;
import com.medibook.medibookservice.entity.User;

public interface UserService {

    UserResponse registerUser(RegisterRequest request);

    /**
     * Login a user with email and password.
     * Generates a JWT token on success.
     */
    AuthResponse login(LoginRequest request);

    User findByEmail(String email);

    boolean existsByEmail(String email);
}