package com.example.fintrack.controller;

import com.example.fintrack.dto.CreateUserRequest;
import com.example.fintrack.dto.UpdateUserRequest;
import com.example.fintrack.dto.UserResponse;
import com.example.fintrack.dto.ChangePasswordRequest;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import com.example.fintrack.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import com.example.fintrack.repository.UserRepository;
import com.example.fintrack.exception.ResourceNotFoundException;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController {
    private final UserService userService;
    private final UserRepository userRepository;

    private void requireOwner(Long id, Jwt jwt) {
        if (!userRepository.existsByIdAndEmailIgnoreCase(id, jwt.getSubject())) {
            throw new ResourceNotFoundException("User not found");
        }
    }

    @GetMapping("/me")
    public ResponseEntity<UserResponse> me(@AuthenticationPrincipal Jwt jwt) {
        Long userId = ((Number) jwt.getClaim("uid")).longValue();
        return ResponseEntity.ok(userService.findById(userId));
    }

    @PutMapping("/me")
    public ResponseEntity<UserResponse> updateMe(@Valid @RequestBody UpdateProfileRequest user,
                                                  @AuthenticationPrincipal Jwt jwt) {
        Long userId = ((Number) jwt.getClaim("uid")).longValue();
        return ResponseEntity.ok(userService.updateName(userId, user.name()));
    }

    @PostMapping("/me/password")
    public ResponseEntity<Void> changePassword(@Valid @RequestBody ChangePasswordRequest request,
                                               @AuthenticationPrincipal Jwt jwt) {
        Long userId = ((Number) jwt.getClaim("uid")).longValue();
        userService.changePassword(userId, request.currentPassword(), request.newPassword());
        return ResponseEntity.noContent().build();
    }

    public record UpdateProfileRequest(@NotBlank @Size(max = 100) String name) { }

    @GetMapping("/{id}")
    public ResponseEntity< UserResponse> findById(@PathVariable Long id, @AuthenticationPrincipal Jwt jwt){
        requireOwner(id, jwt);
        return ResponseEntity.ok(userService.findById(id));
    }

    @PostMapping
    public ResponseEntity<Void> save(@Valid @RequestBody CreateUserRequest user) {
        userService.save(user);
        return ResponseEntity.status(HttpStatus.CREATED).build();

    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id, @AuthenticationPrincipal Jwt jwt){
        requireOwner(id, jwt);
        userService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{id}")
    public ResponseEntity<UserResponse> update(@PathVariable Long id, @Valid @RequestBody UpdateUserRequest user, @AuthenticationPrincipal Jwt jwt){
       requireOwner(id, jwt);
       UserResponse userResponse=userService.update(id,user);
       return ResponseEntity.ok(userResponse);
    }
}
