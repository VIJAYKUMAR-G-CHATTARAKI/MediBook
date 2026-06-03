package com.medibook.medibookservice.controller;

import com.medibook.medibookservice.dto.request.LoginRequest;
import com.medibook.medibookservice.dto.request.RegisterRequest;
import com.medibook.medibookservice.dto.response.ApiResponse;
import com.medibook.medibookservice.dto.response.AuthResponse;
import com.medibook.medibookservice.dto.response.UserResponse;
import com.medibook.medibookservice.service.UserService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * REST controller for authentication operations (register, login).
 * All endpoints are publicly accessible (no auth required).
 */
@RestController
@RequestMapping("/auth")
public class AuthController {

    private static final Logger log = LoggerFactory.getLogger(AuthController.class);

    private final UserService userService;

    // Constructor injection
    public AuthController(UserService userService) {
        this.userService = userService;
    }

    /**
     * POST /api/v1/auth/register
     * Registers a new user (Patient/Doctor/Admin).
     *
     * @param request the registration details (validated)
     * @return 201 CREATED with the registered user info
     */
    @PostMapping("/register")
    public ResponseEntity<ApiResponse<UserResponse>> register(
            @Valid @RequestBody RegisterRequest request) {

        log.info("Received registration request for email: {}", request.getEmail());

        UserResponse userResponse = userService.registerUser(request);

        ApiResponse<UserResponse> response = ApiResponse.success(
                "User registered successfully",
                userResponse
        );

        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    /**
     * POST /api/v1/auth/login
     */
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AuthResponse>> login(
            @Valid @RequestBody LoginRequest request) {

        log.info("Received login request for email: {}", request.getEmail());
        AuthResponse authResponse = userService.login(request);

        ApiResponse<AuthResponse> response = ApiResponse.success(
                "Login successful",
                authResponse
        );
        return new ResponseEntity<>(response, HttpStatus.OK);
    }
}