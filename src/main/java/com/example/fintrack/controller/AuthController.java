package com.example.fintrack.controller;

import com.example.fintrack.dto.AuthResponse;
import com.example.fintrack.dto.LoginRequest;
import com.example.fintrack.dto.CreateUserRequest;
import com.example.fintrack.service.AuthService;
import com.example.fintrack.service.EmailVerificationService;
import com.example.fintrack.dto.VerifyEmailRequest;
import com.example.fintrack.dto.ResendVerificationRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {
    private final AuthService authService;
    private final EmailVerificationService verificationService;

    @PostMapping("/register")
    public ResponseEntity<Void> register(@Valid @RequestBody CreateUserRequest request) {
        verificationService.begin(request);
        return ResponseEntity.accepted().build();
    }

    @PostMapping("/verify-email")
    public ResponseEntity<Void> verifyEmail(@Valid @RequestBody VerifyEmailRequest request) {
        verificationService.verify(request.email(), request.code());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/resend-verification")
    public ResponseEntity<Void> resendVerification(@Valid @RequestBody ResendVerificationRequest request) {
        verificationService.resend(request.email());
        return ResponseEntity.accepted().build();
    }

    @PostMapping("/login")
    public AuthResponse login(@Valid @RequestBody LoginRequest request) {
        return authService.login(request);
    }
}
