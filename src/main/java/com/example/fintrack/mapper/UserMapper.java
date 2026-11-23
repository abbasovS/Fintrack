package com.example.fintrack.mapper;

import com.example.fintrack.dto.CreateUserRequest;
import com.example.fintrack.dto.UserResponse;
import com.example.fintrack.entity.User;
import org.springframework.stereotype.Component;
@Component
public class UserMapper {

    public User toEntity(CreateUserRequest request) {
        if (request == null) {
            return null;
        }

        return User.builder()
                .name(request.name())
                .email(request.email())
                .password(request.password())
                .build();
    }

    public UserResponse toResponse(User entity) {
        if (entity == null) {
            return null;
        }

        return UserResponse.builder()
                .id(entity.getId())
                .name(entity.getName())
                .email(entity.getEmail())
                .createdAt(entity.getCreatedAt())
                .build();
    }
}
