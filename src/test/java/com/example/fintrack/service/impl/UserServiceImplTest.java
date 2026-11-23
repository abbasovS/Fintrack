package com.example.fintrack.service.impl;

import com.example.fintrack.dto.CreateUserRequest;
import com.example.fintrack.dto.UpdateUserRequest;
import com.example.fintrack.dto.UserResponse;
import com.example.fintrack.entity.User;
import com.example.fintrack.exception.EmailAlreadyExistsException;
import com.example.fintrack.exception.ResourceNotFoundException;
import com.example.fintrack.mapper.UserMapper;
import com.example.fintrack.repository.CategoryRepository;
import com.example.fintrack.repository.SubscriptionCategoryRepository;
import com.example.fintrack.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceImplTest {
    @Mock private UserRepository userRepository;
    @Mock private CategoryRepository categoryRepository;
    @Mock private SubscriptionCategoryRepository subscriptionCategoryRepository;
    @Mock private UserMapper userMapper;
    @Mock private PasswordEncoder passwordEncoder;
    @InjectMocks private UserServiceImpl userService;

    @Test
    void findById_shouldReturnUser() {
        User user = user(1L, "Ali", "ali@test.com", "hash");
        UserResponse response = UserResponse.builder().id(1L).name("Ali").email("ali@test.com").build();
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(userMapper.toResponse(user)).thenReturn(response);

        assertEquals(response, userService.findById(1L));
    }

    @Test
    void findById_shouldThrowWhenMissing() {
        when(userRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> userService.findById(1L));
    }

    @Test
    void save_shouldNormalizeEmailEncodePasswordAndCreateDefaults() {
        CreateUserRequest request = new CreateUserRequest("Ali", " ALI@Test.com ", "password123");
        User entity = user(1L, "Ali", "ALI@Test.com", "password123");
        when(userRepository.existsByEmailIgnoreCase("ali@test.com")).thenReturn(false);
        when(userMapper.toEntity(request)).thenReturn(entity);
        when(passwordEncoder.encode("password123")).thenReturn("encoded");
        when(userRepository.save(entity)).thenReturn(entity);

        userService.save(request);

        assertEquals("ali@test.com", entity.getEmail());
        assertEquals("encoded", entity.getPassword());
        verify(categoryRepository).saveAll(any());
        verify(subscriptionCategoryRepository).saveAll(any());
    }

    @Test
    void save_shouldRejectDuplicateEmail() {
        CreateUserRequest request = new CreateUserRequest("Ali", "Ali@Test.com", "password123");
        when(userRepository.existsByEmailIgnoreCase("ali@test.com")).thenReturn(true);

        assertThrows(EmailAlreadyExistsException.class, () -> userService.save(request));
        verify(userRepository, never()).save(any());
    }

    @Test
    void changePassword_shouldRejectWrongCurrentPassword() {
        User user = user(1L, "Ali", "ali@test.com", "old-hash");
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("wrong", "old-hash")).thenReturn(false);

        assertThrows(BadCredentialsException.class,
                () -> userService.changePassword(1L, "wrong", "new-password"));
        verify(userRepository, never()).save(any());
    }

    @Test
    void changePassword_shouldEncodeAndSaveNewPassword() {
        User user = user(1L, "Ali", "ali@test.com", "old-hash");
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("old", "old-hash")).thenReturn(true);
        when(passwordEncoder.encode("new-password")).thenReturn("new-hash");

        userService.changePassword(1L, "old", "new-password");

        assertEquals("new-hash", user.getPassword());
        verify(userRepository).save(user);
    }

    @Test
    void update_shouldNormalizeEmailAndName() {
        User user = user(1L, "Old", "old@test.com", "hash");
        UpdateUserRequest request = new UpdateUserRequest(" New Name ", " NEW@Test.com ");
        UserResponse response = UserResponse.builder().id(1L).name(" New Name ").email("new@test.com").build();
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(userRepository.existsByEmailIgnoreCase("new@test.com")).thenReturn(false);
        when(userRepository.save(user)).thenReturn(user);
        when(userMapper.toResponse(user)).thenReturn(response);

        assertEquals(response, userService.update(1L, request));
        assertEquals("new@test.com", user.getEmail());
    }

    @Test
    void delete_shouldRejectMissingUser() {
        when(userRepository.existsById(1L)).thenReturn(false);

        assertThrows(ResourceNotFoundException.class, () -> userService.delete(1L));
        verify(userRepository, never()).deleteById(1L);
    }

    private User user(Long id, String name, String email, String password) {
        return User.builder().id(id).name(name).email(email).password(password).build();
    }
}
