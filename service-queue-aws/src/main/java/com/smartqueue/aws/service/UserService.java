package com.smartqueue.aws.service;

import com.smartqueue.aws.dto.user.CreateUserRequest;
import com.smartqueue.aws.dto.user.UserResponse;
import com.smartqueue.aws.model.User;
import com.smartqueue.aws.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserService {
    
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public Mono<UserResponse> createUser(CreateUserRequest request) {
        log.info("Creating new user with email: {}", request.getEmail());
        
        return Mono.fromCallable(() -> {
            // Check if user already exists
            Optional<User> existingUser = userRepository.findByEmail(request.getEmail());
            if (existingUser.isPresent()) {
                throw new RuntimeException("Email already registered: " + request.getEmail());
            }

            User user = User.builder()
                    .userId(UUID.randomUUID().toString())
                    .email(request.getEmail())
                    .phone(request.getPhone())
                    .name(request.getName())
                    .password(passwordEncoder.encode(request.getPassword()))
                    .emailNotificationEnabled(request.isEmailNotificationEnabled())
                    .smsNotificationEnabled(request.isSmsNotificationEnabled())
                    .createdAt(LocalDateTime.now())
                    .lastLoginAt(LocalDateTime.now())
                    .isActive(true)
                    .build();

            userRepository.save(user);
            
            return UserResponse.builder()
                    .userId(user.getUserId())
                    .email(user.getEmail())
                    .phone(user.getPhone())
                    .name(user.getName())
                    .emailNotificationEnabled(user.isEmailNotificationEnabled())
                    .smsNotificationEnabled(user.isSmsNotificationEnabled())
                    .createdAt(user.getCreatedAt())
                    .lastLoginAt(user.getLastLoginAt())
                    .isActive(user.isActive())
                    .build();
        })
        .doOnSuccess(userResponse -> 
            log.info("Successfully created user: {}", userResponse.getUserId())
        )
        .doOnError(error -> 
            log.error("Failed to create user with email: {}", request.getEmail(), error)
        );
    }

    public Mono<UserResponse> getUserById(String userId) {
        log.debug("Fetching user by ID: {}", userId);
        
        return Mono.fromCallable(() -> {
            return userRepository.findById(userId)
                    .map(user -> UserResponse.builder()
                            .userId(user.getUserId())
                            .email(user.getEmail())
                            .phone(user.getPhone())
                            .name(user.getName())
                            .emailNotificationEnabled(user.isEmailNotificationEnabled())
                            .smsNotificationEnabled(user.isSmsNotificationEnabled())
                            .createdAt(user.getCreatedAt())
                            .lastLoginAt(user.getLastLoginAt())
                            .isActive(user.isActive())
                            .build())
                    .orElseThrow(() -> new RuntimeException("User not found with ID: " + userId));
        });
    }

    public Mono<Void> updateLastLogin(String userId) {
        log.debug("Updating last login for user: {}", userId);
        
        return Mono.fromRunnable(() -> {
            userRepository.findById(userId)
                    .ifPresent(user -> {
                        user.setLastLoginAt(LocalDateTime.now());
                        userRepository.save(user);
                    });
        });
    }

    public Mono<UserResponse> updateNotificationPreferences(String userId, boolean emailEnabled, boolean smsEnabled) {
        log.debug("Updating notification preferences for user: {}", userId);
        
        return Mono.fromCallable(() -> {
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new RuntimeException("User not found with ID: " + userId));
            
            user.setEmailNotificationEnabled(emailEnabled);
            user.setSmsNotificationEnabled(smsEnabled);
            userRepository.save(user);
            
            return UserResponse.builder()
                    .userId(user.getUserId())
                    .email(user.getEmail())
                    .phone(user.getPhone())
                    .name(user.getName())
                    .emailNotificationEnabled(user.isEmailNotificationEnabled())
                    .smsNotificationEnabled(user.isSmsNotificationEnabled())
                    .createdAt(user.getCreatedAt())
                    .lastLoginAt(user.getLastLoginAt())
                    .isActive(user.isActive())
                    .build();
        });
    }

    public Mono<UserResponse> authenticateUser(String email, String password) {
        log.info("🔐 Authenticating user with email: {}", email);

        return Mono.fromCallable(() -> {
            log.debug("Looking up user from repository...");
            User user = userRepository.findByEmail(email)
                    .orElse(null);

            if (user == null) {
                log.warn("❌ User not found with email: {}", email);
                return null;
            }

            if (!user.isActive()) {
                log.warn("❌ User account is inactive: {}", email);
                return null;
            }

            if (!passwordEncoder.matches(password, user.getPassword())) {
                log.warn("❌ Invalid password for user: {}", email);
                return null;
            }

            log.info("✅ User authenticated successfully: {}", email);
            UserResponse response = UserResponse.builder()
                    .userId(user.getUserId())
                    .email(user.getEmail())
                    .phone(user.getPhone())
                    .name(user.getName())
                    .emailNotificationEnabled(user.isEmailNotificationEnabled())
                    .smsNotificationEnabled(user.isSmsNotificationEnabled())
                    .createdAt(user.getCreatedAt())
                    .lastLoginAt(user.getLastLoginAt())
                    .isActive(user.isActive())
                    .build();
            log.debug("Built UserResponse: {}", response);
            return response;
        })
        .timeout(java.time.Duration.ofSeconds(10))
        .flatMap(response -> {
            if (response == null) {
                log.error("❌ Authentication returned null response");
                return Mono.error(new RuntimeException("Invalid credentials"));
            }
            log.debug("✅ Returning authenticated user response");
            return Mono.just(response);
        })
        .onErrorResume(e -> {
            log.error("❌ Authentication error: {}", e.getMessage(), e);
            return Mono.error(new RuntimeException("Authentication failed: " + e.getMessage()));
        });
    }
}