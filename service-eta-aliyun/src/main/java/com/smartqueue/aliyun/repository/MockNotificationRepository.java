package com.smartqueue.aliyun.repository;

import com.smartqueue.aliyun.model.NotificationLog;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Slf4j
@Repository
@Profile("dev") // Chỉ sử dụng trong development
public class MockNotificationRepository {
    
    // In-memory storage for demo purposes
    private final ConcurrentMap<String, NotificationLog> notificationLogs = new ConcurrentHashMap<>();
    
    public NotificationLog save(NotificationLog notificationLog) {
        log.debug("Saving notification log: {}", notificationLog.getNotificationId());
        
        if (notificationLog.getNotificationId() == null) {
            notificationLog.setNotificationId(UUID.randomUUID().toString());
        }
        
        notificationLog.setScheduledAt(Instant.now());
        notificationLogs.put(notificationLog.getNotificationId(), notificationLog);
        
        log.info("Notification log saved: {}", notificationLog.getNotificationId());
        return notificationLog;
    }
    
    public void enqueueNotification(NotificationLog notificationLog) {
        log.debug("Enqueueing notification (MOCK): {}", notificationLog.getNotificationId());
        
        try {
            // Mock enqueueing - just log the notification
            log.info("MOCK ENQUEUE: Notification {} for ticket {} via {} to {}",
                    notificationLog.getNotificationId(),
                    notificationLog.getTicketId(),
                    notificationLog.getChannel(),
                    notificationLog.getRecipient());
            
            // Update status
            notificationLog.setStatus(NotificationLog.NotificationStatus.PENDING);
            save(notificationLog);
            
            // Simulate immediate processing for development
            processNotificationDirectly(notificationLog);
            
        } catch (Exception e) {
            log.error("Error enqueueing notification (MOCK): {}", notificationLog.getNotificationId(), e);
            
            notificationLog.setStatus(NotificationLog.NotificationStatus.FAILED);
            notificationLog.setErrorMessage(e.getMessage());
            save(notificationLog);
        }
    }
    
    private void processNotificationDirectly(NotificationLog notificationLog) {
        try {
            // Mock processing
            log.info("MOCK PROCESSING: Sending {} notification to {} for ticket {}",
                    notificationLog.getChannel(),
                    notificationLog.getRecipient(),
                    notificationLog.getTicketId());
            
            // Simulate successful sending
            notificationLog.setStatus(NotificationLog.NotificationStatus.SENT);
            notificationLog.setSentAt(Instant.now());
            save(notificationLog);
            
        } catch (Exception e) {
            log.error("Error processing notification (MOCK): {}", notificationLog.getNotificationId(), e);
            notificationLog.setStatus(NotificationLog.NotificationStatus.FAILED);
            notificationLog.setErrorMessage(e.getMessage());
            save(notificationLog);
        }
    }
    
    public void updateNotificationStatus(String notificationId, NotificationLog.NotificationStatus status, String errorMessage) {
        log.debug("Updating notification status: {} to {}", notificationId, status);
        
        NotificationLog notificationLog = notificationLogs.get(notificationId);
        if (notificationLog != null) {
            notificationLog.setStatus(status);
            if (status == NotificationLog.NotificationStatus.SENT) {
                notificationLog.setSentAt(Instant.now());
            }
            if (errorMessage != null) {
                notificationLog.setErrorMessage(errorMessage);
            }
            
            notificationLogs.put(notificationId, notificationLog);
            log.info("Notification status updated: {}", notificationId);
        } else {
            log.warn("Notification log not found: {}", notificationId);
        }
    }
    
    public NotificationLog findById(String notificationId) {
        return notificationLogs.get(notificationId);
    }
}