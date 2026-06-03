package com.medibook.medibookservice.mapper;

import com.medibook.medibookservice.dto.request.RegisterRequest;
import com.medibook.medibookservice.dto.response.UserResponse;
import com.medibook.medibookservice.entity.User;
import org.springframework.stereotype.Component;

/**
 * Mapper to convert between User entity and DTOs.
 * Keeps conversion logic in one place (DRY principle).
 */
@Component
public class UserMapper {

    /**
     * Converts a RegisterRequest DTO to a User entity.
     * Note: Password should be HASHED separately in the service layer.
     *
     * @param request the registration request
     * @return User entity (with raw password — service will hash it)
     */
    public User toEntity(RegisterRequest request) {
        if (request == null) {
            return null;
        }

        User user = new User();
        user.setEmail(request.getEmail());
        user.setPassword(request.getPassword());   // Will be hashed in service
        user.setFullName(request.getFullName());
        user.setPhoneNumber(request.getPhoneNumber());
        user.setRole(request.getRole());
        user.setEnabled(true);                     // Active by default
        return user;
    }

    /**
     * Converts a User entity to a UserResponse DTO.
     * Excludes sensitive fields like password.
     *
     * @param user the user entity
     * @return UserResponse DTO (safe to send to client)
     */
    public UserResponse toResponse(User user) {
        if (user == null) {
            return null;
        }

        UserResponse response = new UserResponse();
        response.setId(user.getId());
        response.setEmail(user.getEmail());
        response.setFullName(user.getFullName());
        response.setPhoneNumber(user.getPhoneNumber());
        response.setRole(user.getRole());
        response.setEnabled(user.isEnabled());
        response.setCreatedAt(user.getCreatedAt());
        return response;
    }
}