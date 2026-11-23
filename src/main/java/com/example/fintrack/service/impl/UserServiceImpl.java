package com.example.fintrack.service.impl;

import com.example.fintrack.dto.CreateUserRequest;
import com.example.fintrack.dto.UserResponse;
import com.example.fintrack.dto.UpdateUserRequest;
import com.example.fintrack.entity.User;
import com.example.fintrack.entity.Category;
import com.example.fintrack.entity.CategoryType;
import com.example.fintrack.entity.SubscriptionCategory;
import com.example.fintrack.exception.ResourceNotFoundException;
import com.example.fintrack.exception.EmailAlreadyExistsException;
import com.example.fintrack.mapper.UserMapper;
import com.example.fintrack.repository.UserRepository;
import com.example.fintrack.repository.CategoryRepository;
import com.example.fintrack.repository.SubscriptionCategoryRepository;
import com.example.fintrack.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class UserServiceImpl implements UserService {
    private final UserRepository userRepository;
    private final CategoryRepository categoryRepository;
    private final SubscriptionCategoryRepository subscriptionCategoryRepository;
    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;

    @Override
    public UserResponse findById(Long id) {
        User user=userRepository.findById(id).orElseThrow(()->new ResourceNotFoundException("User not found"));
        return userMapper.toResponse(user);
    }

    @Override
    public void save(CreateUserRequest user) {
     String normalizedEmail = user.email().trim().toLowerCase(Locale.ROOT);
     if (userRepository.existsByEmailIgnoreCase(normalizedEmail)) throw new EmailAlreadyExistsException("Email is already registered");
     User entity = userMapper.toEntity(user);
     entity.setEmail(normalizedEmail);
     entity.setPassword(passwordEncoder.encode(user.password()));
     entity.setEmailVerified(true);
     entity = userRepository.save(entity);
     createDefaultCategories(entity);
    }

    @Override
    public void saveVerified(String name, String email, String encodedPassword) {
        String normalizedEmail = email.trim().toLowerCase(Locale.ROOT);
        if (userRepository.existsByEmailIgnoreCase(normalizedEmail)) throw new EmailAlreadyExistsException("Email is already registered");
        User entity = User.builder().name(name.trim()).email(normalizedEmail).password(encodedPassword).emailVerified(true).build();
        entity = userRepository.save(entity);
        createDefaultCategories(entity);
    }

    @Override
    public void changePassword(Long id, String currentPassword, String newPassword) {
        User user = userRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("User not found"));
        if (!passwordEncoder.matches(currentPassword, user.getPassword())) {
            throw new org.springframework.security.authentication.BadCredentialsException("Current password is incorrect");
        }
        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);
    }

    @Override
    public UserResponse updateName(Long id, String name) {
        User user = userRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("User not found"));
        user.setName(name.trim());
        return userMapper.toResponse(userRepository.save(user));
    }

    private void createDefaultCategories(User user) {
        List<Category> categories = List.of(
                Category.builder().name("Maaş").type(CategoryType.INCOME).user(user).build(),
                Category.builder().name("Bonus və mükafat").type(CategoryType.INCOME).user(user).build(),
                Category.builder().name("Freelance").type(CategoryType.INCOME).user(user).build(),
                Category.builder().name("Digər gəlir").type(CategoryType.INCOME).user(user).build(),
                Category.builder().name("Ərzaq").type(CategoryType.EXPENSE).user(user).build(),
                Category.builder().name("Nəqliyyat").type(CategoryType.EXPENSE).user(user).build(),
                Category.builder().name("Ev və kommunal").type(CategoryType.EXPENSE).user(user).build(),
                Category.builder().name("Sağlamlıq").type(CategoryType.EXPENSE).user(user).build(),
                Category.builder().name("Əyləncə").type(CategoryType.EXPENSE).user(user).build(),
                Category.builder().name("Digər xərc").type(CategoryType.EXPENSE).user(user).build());
        categoryRepository.saveAll(categories);

        List<String> subscriptionCategories = List.of(
                "Video və musiqi", "İdman və sağlamlıq", "Proqram təminatı və bulud",
                "Rabitə və internet", "Təhsil", "Nəqliyyat", "Ev xidmətləri", "Digər");
        subscriptionCategoryRepository.saveAll(subscriptionCategories.stream()
                .map(name -> SubscriptionCategory.builder().name(name).user(user).build())
                .toList());
    }

    @Override
    public void delete(Long id) {
      if (!userRepository.existsById(id)) throw new ResourceNotFoundException("User not found");
      userRepository.deleteById(id);
    }

    @Override
    public UserResponse update(Long id, UpdateUserRequest user) {
        User user1=userRepository.findById(id).orElseThrow(()->new ResourceNotFoundException("User not found"));
        String normalizedEmail = user.email().trim().toLowerCase(Locale.ROOT);
        if (!user1.getEmail().equalsIgnoreCase(normalizedEmail) && userRepository.existsByEmailIgnoreCase(normalizedEmail))
            throw new EmailAlreadyExistsException("Email is already registered");
        user1.setName(user.name());
        user1.setEmail(normalizedEmail);
        User updatedUser=userRepository.save(user1);
        return userMapper.toResponse(updatedUser);
    }
}
