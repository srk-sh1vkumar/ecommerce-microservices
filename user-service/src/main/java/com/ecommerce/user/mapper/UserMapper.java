package com.ecommerce.user.mapper;

import com.ecommerce.user.dto.UserResponseDTO;
import com.ecommerce.user.entity.User;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

/**
 * MapStruct mapper for User entity to DTO conversions.
 * Automatically generates implementation at compile time.
 */
@Mapper(componentModel = "spring")
public interface UserMapper {

    UserMapper INSTANCE = Mappers.getMapper(UserMapper.class);

    /**
     * Convert User entity to UserResponseDTO (excludes password).
     */
    @Mapping(target = "id", source = "id")
    @Mapping(target = "email", source = "email")
    @Mapping(target = "firstName", source = "firstName")
    @Mapping(target = "lastName", source = "lastName")
    @Mapping(target = "role", source = "role")
    @Mapping(target = "createdAt", source = "createdAt")
    UserResponseDTO toResponseDTO(User user);

    /**
     * Convert User entity to UserResponseDTO without password.
     * Alias method for clarity.
     */
    default UserResponseDTO toDTO(User user) {
        return toResponseDTO(user);
    }
}
