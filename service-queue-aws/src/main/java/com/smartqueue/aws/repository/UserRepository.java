package com.smartqueue.aws.repository;

import com.smartqueue.aws.model.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.Key;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;

import java.util.Optional;

@Repository
@RequiredArgsConstructor
@Slf4j
public class UserRepository {
    
    private final DynamoDbEnhancedClient dynamoDbEnhancedClient;
    private static final String TABLE_NAME = "smartqueue-users";

    private DynamoDbTable<User> getUserTable() {
        return dynamoDbEnhancedClient.table(TABLE_NAME, TableSchema.fromBean(User.class));
    }

    public User save(User user) {
        log.debug("Saving user: {}", user.getUserId());
        getUserTable().putItem(user);
        return user;
    }

    public Optional<User> findById(String userId) {
        log.debug("Finding user by ID: {}", userId);
        Key key = Key.builder().partitionValue(userId).build();
        User user = getUserTable().getItem(key);
        return Optional.ofNullable(user);
    }

    public void delete(String userId) {
        log.debug("Deleting user: {}", userId);
        Key key = Key.builder().partitionValue(userId).build();
        getUserTable().deleteItem(key);
    }

    public Optional<User> findByEmail(String email) {
        // Note: In real implementation, you'd need GSI for email lookup
        // For now, this is a placeholder - in production use GSI
        log.debug("Finding user by email: {} (placeholder implementation)", email);
        return Optional.empty();
    }
}