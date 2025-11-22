package com.smartqueue.aws.repository;

import com.smartqueue.aws.model.User;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Repository;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory implementation of UserRepository for development/testing
 * Used when app.use-in-memory=true
 */
@Repository
@Primary
@ConditionalOnProperty(name = "app.use-in-memory", havingValue = "true")
@Slf4j
public class InMemoryUserRepository extends UserRepository {
    
    private final Map<String, User> userStore = new ConcurrentHashMap<>();
    private final Map<String, User> emailIndex = new ConcurrentHashMap<>();

    public InMemoryUserRepository() {
        super(null); // No DynamoDB client needed
        log.info("🧪 InMemoryUserRepository initialized for development mode");
    }

    @Override
    public User save(User user) {
        log.debug("💾 [IN-MEMORY] Saving user: {}", user.getUserId());
        userStore.put(user.getUserId(), user);
        emailIndex.put(user.getEmail().toLowerCase(), user);
        log.info("✅ [IN-MEMORY] User saved: {} ({})", user.getEmail(), user.getUserId());
        return user;
    }

    @Override
    public Optional<User> findById(String userId) {
        log.debug("🔍 [IN-MEMORY] Finding user by ID: {}", userId);
        User user = userStore.get(userId);
        if (user != null) {
            log.debug("✅ [IN-MEMORY] User found: {}", userId);
        } else {
            log.debug("❌ [IN-MEMORY] User not found: {}", userId);
        }
        return Optional.ofNullable(user);
    }

    @Override
    public void delete(String userId) {
        log.debug("🗑️  [IN-MEMORY] Deleting user: {}", userId);
        User user = userStore.remove(userId);
        if (user != null) {
            emailIndex.remove(user.getEmail().toLowerCase());
            log.info("✅ [IN-MEMORY] User deleted: {}", userId);
        }
    }

    @Override
    public Optional<User> findByEmail(String email) {
        log.debug("🔍 [IN-MEMORY] Finding user by email: {}", email);
        User user = emailIndex.get(email.toLowerCase());
        if (user != null) {
            log.debug("✅ [IN-MEMORY] User found by email: {}", email);
        } else {
            log.debug("❌ [IN-MEMORY] User not found by email: {}", email);
        }
        return Optional.ofNullable(user);
    }
    
    // Helper method for testing
    public void clear() {
        userStore.clear();
        emailIndex.clear();
        log.info("🧹 [IN-MEMORY] User store cleared");
    }
    
    public int count() {
        return userStore.size();
    }
}
