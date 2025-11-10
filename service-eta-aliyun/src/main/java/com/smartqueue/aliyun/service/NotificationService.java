package com.smartqueue.aliyun.service;

import com.smartqueue.aliyun.dto.request.NotificationRequest;
import com.smartqueue.aliyun.dto.response.NotificationResponse;
import com.smartqueue.aliyun.model.NotificationLog;
import com.smartqueue.aliyun.repository.NotificationRepository;
import com.smartqueue.aliyun.repository.MockNotificationRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Slf4j
@Service
public class NotificationService {
    
    @Autowired(required = false)
    private NotificationRepository notificationRepository;
    
    @Autowired(required = false)
    private MockNotificationRepository mockNotificationRepository;
    
    @Value("${notification.mode:QUEUE}")
    private String notificationMode;
    
    @Value("${notification.eta-threshold-minutes:10}")
    private int etaThresholdMinutes;
    
    public NotificationResponse scheduleNotification(NotificationRequest request) {
        log.info("Scheduling notification for ticketId: {}, channel: {}", request.getTicketId(), request.getChannel());
        
        try {
            NotificationLog notificationLog = NotificationLog.builder()
                    .notificationId(UUID.randomUUID().toString())
                    .ticketId(request.getTicketId())
                    .channel(convertToNotificationChannel(request.getChannel()))
                    .recipient(request.getAddress())
                    .message(request.getMessage() != null ? request.getMessage() : "Your turn is coming up soon!")
                    .status(NotificationLog.NotificationStatus.PENDING)
                    .build();
            
            // Save notification log - use mock repository in dev profile
            if (mockNotificationRepository != null) {
                notificationLog = mockNotificationRepository.save(notificationLog);
                // Always enqueue for mock (it processes immediately)
                mockNotificationRepository.enqueueNotification(notificationLog);
            } else {
                notificationLog = notificationRepository.save(notificationLog);
                if ("DIRECT".equalsIgnoreCase(notificationMode)) {
                    sendNotificationDirect(notificationLog);
                } else {
                    notificationRepository.enqueueNotification(notificationLog);
                }
            }
            
            log.info("Notification scheduled successfully: {}", notificationLog.getNotificationId());
            
            return NotificationResponse.builder()
                    .notificationId(notificationLog.getNotificationId())
                    .ticketId(request.getTicketId())
                    .scheduled(true)
                    .status("SCHEDULED")
                    .message("Notification scheduled successfully")
                    .build();
                    
        } catch (Exception e) {
            log.error("Error scheduling notification for ticketId: {}", request.getTicketId(), e);
            
            return NotificationResponse.builder()
                    .ticketId(request.getTicketId())
                    .scheduled(false)
                    .status("FAILED")
                    .message("Failed to schedule notification: " + e.getMessage())
                    .build();
        }
    }
    
    public void processNotificationQueue() {
        log.debug("Processing notification queue - using mock implementation");
        
        try {
            // Mock queue processing for now
            // In production, integrate with Aliyun MNS
            log.info("Processing notification queue (mock implementation)");
        } catch (Exception e) {
            log.error("Error processing notification queue", e);
        }
    }
    
    private void sendNotificationDirect(NotificationLog notificationLog) {
        log.info("Sending notification directly: {}", notificationLog.getNotificationId());
        
        try {
            switch (notificationLog.getChannel()) {
                case EMAIL:
                    sendEmail(notificationLog);
                    break;
                case SMS:
                    sendSms(notificationLog);
                    break;
                case PUSH:
                    sendPushNotification(notificationLog);
                    break;
                default:
                    throw new RuntimeException("Unsupported notification channel: " + notificationLog.getChannel());
            }
            
            notificationRepository.updateNotificationStatus(
                notificationLog.getNotificationId(), 
                NotificationLog.NotificationStatus.SENT, 
                null
            );
            
            log.info("Notification sent successfully: {}", notificationLog.getNotificationId());
            
        } catch (Exception e) {
            log.error("Error sending notification: {}", notificationLog.getNotificationId(), e);
            
            notificationRepository.updateNotificationStatus(
                notificationLog.getNotificationId(), 
                NotificationLog.NotificationStatus.FAILED, 
                e.getMessage()
            );
        }
    }
    
    private void sendEmail(NotificationLog notificationLog) {
        log.info("Sending email to: {}", notificationLog.getRecipient());
        
        // Mock email sending for now
        // In production, use Aliyun DirectMail service
        log.info("EMAIL SENT: To={}, Message={}", notificationLog.getRecipient(), notificationLog.getMessage());
    }
    
    private void sendSms(NotificationLog notificationLog) {
        log.info("Sending SMS to: {}", notificationLog.getRecipient());
        
        // Mock SMS sending for now
        // In production, use Aliyun SMS service
        log.info("SMS SENT: To={}, Message={}", notificationLog.getRecipient(), notificationLog.getMessage());
    }
    
    private void sendPushNotification(NotificationLog notificationLog) {
        log.info("Sending push notification to: {}", notificationLog.getRecipient());
        
        // Mock push notification for now
        log.info("PUSH SENT: To={}, Message={}", notificationLog.getRecipient(), notificationLog.getMessage());
    }
    
    private void processNotificationMessage(String messageBody) {
        try {
            log.debug("Processing notification message: {}", messageBody);
            
            // For demo purposes, just log the message
            // In production, parse JSON and create NotificationLog object to send
            log.info("Processing queued notification: {}", messageBody);
            
        } catch (Exception e) {
            log.error("Error processing notification message", e);
        }
    }
    
    private NotificationLog.NotificationType convertToNotificationChannel(NotificationRequest.NotificationChannel channel) {
        switch (channel) {
            case EMAIL:
                return NotificationLog.NotificationType.EMAIL;
            case SMS:
                return NotificationLog.NotificationType.SMS;
            case PUSH:
                return NotificationLog.NotificationType.PUSH;
            default:
                throw new RuntimeException("Unsupported channel: " + channel);
        }
    }
}