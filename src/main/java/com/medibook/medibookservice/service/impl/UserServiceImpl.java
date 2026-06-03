package com.medibook.medibookservice.service.impl;

import com.medibook.medibookservice.dto.request.LoginRequest;
import com.medibook.medibookservice.dto.request.RegisterRequest;
import com.medibook.medibookservice.dto.response.AuthResponse;
import com.medibook.medibookservice.dto.response.UserResponse;
import com.medibook.medibookservice.entity.User;
import com.medibook.medibookservice.exception.ResourceAlreadyExistsException;
import com.medibook.medibookservice.exception.ResourceNotFoundException;
import com.medibook.medibookservice.mapper.UserMapper;
import com.medibook.medibookservice.repository.UserRepository;
import com.medibook.medibookservice.security.JwtService;
import com.medibook.medibookservice.service.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserServiceImpl implements UserService {

    private static final Logger log = LoggerFactory.getLogger(UserServiceImpl.class);

    private final UserRepository userRepository;
    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public UserServiceImpl(UserRepository userRepository,
                           UserMapper userMapper,
                           PasswordEncoder passwordEncoder,
                           JwtService jwtService) {
        this.userRepository = userRepository;
        this.userMapper = userMapper;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    @Override
    @Transactional
    public UserResponse registerUser(RegisterRequest request) {
        log.info("Attempting to register user with email: {}", request.getEmail());

        if (userRepository.existsByEmail(request.getEmail())) {
            log.warn("Registration failed - email already exists: {}", request.getEmail());
            throw new ResourceAlreadyExistsException("User", "email", request.getEmail());
        }

        User user = userMapper.toEntity(request);
        user.setPassword(passwordEncoder.encode(request.getPassword()));

        User savedUser = userRepository.save(user);
        log.info("User registered successfully with id: {}", savedUser.getId());

        return userMapper.toResponse(savedUser);
    }

    @Override
    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest request) {
        log.info("Login attempt for email: {}", request.getEmail());

        // 1. Find user by email
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> {
                    log.warn("Login failed - user not found: {}", request.getEmail());
                    return new BadCredentialsException("Invalid email or password");
                });

        // 2. Check if account is enabled
        if (!user.isEnabled()) {
            log.warn("Login failed - account disabled: {}", request.getEmail());
            throw new BadCredentialsException("Account is disabled");
        }

        // 3. Verify password (BCrypt compares hashed values)
        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            log.warn("Login failed - wrong password for: {}", request.getEmail());
            throw new BadCredentialsException("Invalid email or password");
        }

        // 4. Generate JWT token
        String token = jwtService.generateToken(user);

        log.info("Login successful for: {}", request.getEmail());

        // 5. Return auth response with token
        return new AuthResponse(
                token,
                "Bearer",
                user.getId(),
                user.getEmail(),
                user.getFullName(),
                user.getRole()
        );
    }

    @Override
    @Transactional(readOnly = true)
    public User findByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User", "email", email));
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existsByEmail(String email) {
        return userRepository.existsByEmail(email);
    }
}