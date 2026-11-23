package com.example.fintrack.service;

import com.example.fintrack.dto.CreateUserRequest;
import com.example.fintrack.dto.UserResponse;
import com.example.fintrack.dto.UpdateUserRequest;

public interface UserService {
     void save(CreateUserRequest user);
     void saveVerified(String name, String email, String encodedPassword);
     void changePassword(Long id, String currentPassword, String newPassword);
     UserResponse updateName(Long id, String name);
     void delete(Long id);
     UserResponse findById(Long id);
     UserResponse update(Long id, UpdateUserRequest user);
}
