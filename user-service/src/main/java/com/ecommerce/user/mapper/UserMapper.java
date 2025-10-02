package com.ecommerce.user.mapper;

import com.ecommerce.user.dto.UserResponseDTO;
import com.ecommerce.user.entity.User;
import org.springframework.stereotype.Component;

/**
 * Mapper for User entity to DTO conversions.
 */
@Component
public class UserMapper {

    /**
     * Convert User entity to UserResponseDTO (excludes password).
     */
    public UserResponseDTO toResponseDTO(User user) {
        if (user == null) {
            return null;
        }

        UserResponseDTO dto = new UserResponseDTO();
        dto.setId(user.getId());
        dto.setEmail(user.getEmail());
        dto.setFirstName(user.getFirstName());
        dto.setLastName(user.getLastName());
        dto.setRole(user.getRole());
        dto.setCreatedAt(user.getCreatedAt());
        return dto;
    }

    /**
     * Convert User entity to UserResponseDTO without password.
     * Alias method for clarity.
     */
    public UserResponseDTO toDTO(User user) {
        return toResponseDTO(user);
    }
}
