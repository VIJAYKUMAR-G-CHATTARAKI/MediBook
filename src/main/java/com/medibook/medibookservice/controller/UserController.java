package com.medibook.medibookservice.controller;

import com.medibook.medibookservice.dto.response.ApiResponse;
import com.medibook.medibookservice.dto.response.UserResponse;
import com.medibook.medibookservice.entity.User;
import com.medibook.medibookservice.mapper.UserMapper;
import com.medibook.medibookservice.service.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/users")
public class UserController {

    private final UserService userService;
    private final UserMapper userMapper;

    public UserController(UserService userService, UserMapper userMapper) {
        this.userService = userService;
        this.userMapper = userMapper;
    }

    /**
     * GET /api/v1/users/me
     * Returns the current logged-in user's info.
     * Requires a valid JWT token.
     */
    @GetMapping("/me")
    public ResponseEntity<ApiResponse<UserResponse>> getCurrentUser(
            @AuthenticationPrincipal UserDetails userDetails) {

        User user = userService.findByEmail(userDetails.getUsername());
        UserResponse response = userMapper.toResponse(user);

        return ResponseEntity.ok(ApiResponse.success("User details retrieved", response));
    }

    /**
     * GET /api/v1/users/admin-only
     * Test endpoint — only ADMINs can access.
     */
    @GetMapping("/admin-only")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<String>> adminOnly() {
        return ResponseEntity.ok(ApiResponse.success(
                "Access granted",
                "Welcome, Admin! 👑"
        ));
    }
}