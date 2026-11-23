package com.example.fintrack.service;

import com.example.fintrack.dto.CreateUserRequest;
import com.example.fintrack.entity.EmailVerification;
import com.example.fintrack.exception.EmailAlreadyExistsException;
import com.example.fintrack.repository.EmailVerificationRepository;
import com.example.fintrack.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Locale;

@Service
@RequiredArgsConstructor
@Transactional
public class EmailVerificationService {
    private final EmailVerificationRepository verificationRepository;
    private final UserRepository userRepository;
    private final UserService userService;
    private final PasswordEncoder passwordEncoder;
    private final JavaMailSender mailSender;
    private final SecureRandom secureRandom = new SecureRandom();

    @Value("${fintrack.mail.from:no-reply@fintrack.local}")
    private String from;

    public void begin(CreateUserRequest request) {
        String email = normalize(request.email());
        if (userRepository.existsByEmailIgnoreCase(email)) throw new EmailAlreadyExistsException("Email is already registered");
        EmailVerification pending = verificationRepository.findById(email).orElse(null);
        if (pending != null && Duration.between(pending.getLastSentAt(), Instant.now()).toSeconds() < 60) {
            throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS, "Wait a minute before requesting another code");
        }
        sendCode(email, request.name().trim(), passwordEncoder.encode(request.password()), pending);
    }

    public void resend(String rawEmail) {
        String email = normalize(rawEmail);
        EmailVerification pending = verificationRepository.findById(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "No pending verification for this email"));
        if (Duration.between(pending.getLastSentAt(), Instant.now()).toSeconds() < 60) {
            throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS, "Wait a minute before requesting another code");
        }
        sendCode(email, pending.getName(), pending.getPasswordHash(), pending);
    }

    @Transactional(noRollbackFor = ResponseStatusException.class)
    public void verify(String rawEmail, String code) {
        String email = normalize(rawEmail);
        EmailVerification pending = verificationRepository.findById(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "No pending verification for this email"));
        if (pending.getExpiresAt().isBefore(Instant.now())) {
            verificationRepository.delete(pending);
            throw new ResponseStatusException(HttpStatus.GONE, "Verification code expired. Request a new one.");
        }
        if (pending.getAttempts() >= 5) {
            verificationRepository.delete(pending);
            throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS, "Too many incorrect codes. Start registration again.");
        }
        if (!passwordEncoder.matches(code, pending.getCodeHash())) {
            pending.setAttempts(pending.getAttempts() + 1);
            verificationRepository.save(pending);
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Verification code is incorrect");
        }
        userService.saveVerified(pending.getName(), email, pending.getPasswordHash());
        verificationRepository.delete(pending);
    }

    private void sendCode(String email, String name, String passwordHash, EmailVerification previous) {
        String code = String.format(Locale.ROOT, "%06d", secureRandom.nextInt(1_000_000));
        Instant now = Instant.now();
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(from);
        message.setTo(email);
        message.setSubject("FinTrack e-poçt təsdiqi");
        message.setText("Salam " + name + ",\n\nFinTrack qeydiyyatını tamamlamaq üçün təsdiq kodunuz: " + code
                + "\n\nKod 10 dəqiqə ərzində qüvvədədir. Bu sorğunu siz etməmisinizsə, bu məktubu nəzərə almayın.");
        mailSender.send(message);
        EmailVerification verification = previous == null
                ? EmailVerification.builder().email(email).name(name).passwordHash(passwordHash).build()
                : previous;
        verification.setName(name);
        verification.setPasswordHash(passwordHash);
        verification.setCodeHash(passwordEncoder.encode(code));
        verification.setExpiresAt(now.plus(Duration.ofMinutes(10)));
        verification.setLastSentAt(now);
        verification.setAttempts(0);
        verificationRepository.save(verification);
    }

    private String normalize(String email) { return email.trim().toLowerCase(Locale.ROOT); }
}
