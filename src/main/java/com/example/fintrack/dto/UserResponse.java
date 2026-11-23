package com.example.fintrack.dto;

import lombok.Builder;

import java.time.Instant;

@Builder
public record UserResponse (
        Long id,
        String name,
        String email,
        Instant createdAt
){
}
