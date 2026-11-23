package com.example.fintrack.repository;

import com.example.fintrack.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, Long> {
    boolean existsByEmailIgnoreCase(String email);
    java.util.Optional<User> findByEmailIgnoreCase(String email);
    boolean existsByIdAndEmailIgnoreCase(Long id, String email);
}
