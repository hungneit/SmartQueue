package com.smartqueue.aws.controller;

import com.smartqueue.aws.dto.user.CreateUserRequest;
import com.smartqueue.aws.dto.user.LoginRequest;
import com.smartqueue.aws.dto.user.UserResponse;
import com.smartqueue.aws.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
@Validated
@Slf4j
@CrossOrigin(origins = "*")
public class UserController {
    
    private final UserService userService;

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public Mono<UserResponse> registerUser(@Valid @RequestBody CreateUserRequest request) {
        log.info("User registration request for email: {}", request.getEmail());
        return userService.createUser(request);
    }

    @PostMapping("/login")
    public Mono<UserResponse> login(@Valid @RequestBody LoginRequest request) {
        log.info("User login attempt for email: {}", request.getEmail());
        return userService.authenticateUser(request.getEmail(), request.getPassword());
    }


    @GetMapping("/{userId}")
    public Mono<UserResponse> getUser(@PathVariable String userId) {
        log.debug("Fetching user details for ID: {}", userId);
        return userService.getUserById(userId);
    }

    @PutMapping("/{userId}/notifications")
    public Mono<UserResponse> updateNotificationPreferences(
            @PathVariable String userId,
            @RequestParam boolean emailEnabled,
            @RequestParam boolean smsEnabled) {
        log.info("Updating notification preferences for user: {}", userId);
        return userService.updateNotificationPreferences(userId, emailEnabled, smsEnabled);
    }

    @PostMapping("/{userId}/last-login")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public Mono<Void> updateLastLogin(@PathVariable String userId) {
        log.debug("Updating last login for user: {}", userId);
        return userService.updateLastLogin(userId);
    }
}