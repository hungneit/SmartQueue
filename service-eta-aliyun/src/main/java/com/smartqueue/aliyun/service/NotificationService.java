package com.smartqueue.aliyun.service;

import com.aliyuncs.DefaultAcsClient;
import com.aliyuncs.IAcsClient;
import com.aliyuncs.dm.model.v20151123.SingleSendMailRequest;
import com.aliyuncs.dm.model.v20151123.SingleSendMailResponse;
import com.aliyuncs.exceptions.ClientException;
import com.aliyuncs.profile.DefaultProfile;
import com.smartqueue.aliyun.dto.request.NotificationRequest;
import com.smartqueue.aliyun.dto.response.NotificationResponse;
import com.smartqueue.aliyun.model.NotificationLog;
import com.smartqueue.aliyun.repository.NotificationRepository;
import com.smartqueue.aliyun.repository.MockNotificationRepository;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.UUID;

@Slf4j
@Service
public class NotificationService {
    
    @Autowired(required = false)
    private NotificationRepository notificationRepository;
    
    @Autowired(required = false)
    private MockNotificationRepository mockNotificationRepository;
    
    @Value("${notification.mode:DIRECT}")
    private String notificationMode;
    
    @Value("${notification.eta-threshold-minutes:10}")
    private int etaThresholdMinutes;
    
    // 📧 Real Email Service Configuration
    @Value("${aliyun.access-key:demo-key}")
    private String accessKeyId;
    
    @Value("${aliyun.access-secret:demo-secret}")
    private String accessKeySecret;
    
    @Value("${aliyun.region:ap-southeast-1}")
    private String region;
    
    @Value("${aliyun.directmail.from-email:noreply@smartqueue.com}")
    private String fromEmail;
    
    @Value("${aliyun.directmail.from-name:SmartQueue System}")
    private String fromName;
    
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
                    sendRealNotification(notificationLog);
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
    
    // 📧 REAL EMAIL NOTIFICATION với Aliyun DirectMail
    private void sendRealNotification(NotificationLog notificationLog) {
        try {
            switch (notificationLog.getChannel()) {
                case EMAIL:
                    sendRealEmail(notificationLog);
                    break;
                case SMS:
                    sendRealSMS(notificationLog);
                    break;
                default:
                    throw new RuntimeException("Unsupported notification channel: " + notificationLog.getChannel());
            }
            
            notificationRepository.updateNotificationStatus(
                notificationLog.getNotificationId(), 
                NotificationLog.NotificationStatus.SENT, 
                null
            );
            
            log.info("✅ REAL Notification sent successfully: {}", notificationLog.getNotificationId());
            
        } catch (Exception e) {
            log.error("❌ Error sending REAL notification: {}", notificationLog.getNotificationId(), e);
            
            notificationRepository.updateNotificationStatus(
                notificationLog.getNotificationId(), 
                NotificationLog.NotificationStatus.FAILED, 
                e.getMessage()
            );
        }
    }
    
    // 📧 Aliyun DirectMail Integration
    private void sendRealEmail(NotificationLog notificationLog) {
        // Check if using demo credentials
        if (accessKeyId.equals("demo-key") || accessKeySecret.equals("demo-secret")) {
            log.info("🧪 Using demo credentials, simulating email send to: {}", notificationLog.getRecipient());
            return;
        }
        
        try {
            DefaultProfile profile = DefaultProfile.getProfile(region, accessKeyId, accessKeySecret);
            IAcsClient client = new DefaultAcsClient(profile);
            
            SingleSendMailRequest request = new SingleSendMailRequest();
            request.setAccountName(fromEmail);
            request.setFromAlias(fromName);
            request.setAddressType(1);
            request.setToAddress(notificationLog.getRecipient());
            request.setReplyToAddress(false);
            request.setSubject("🎯 SmartQueue Notification - Your Turn is Coming!");
            
            // Professional HTML Email Template
            String htmlBody = createEmailHtmlBody(notificationLog);
            request.setHtmlBody(htmlBody);
            
            SingleSendMailResponse response = client.getAcsResponse(request);
            log.info("📧 Email sent successfully via Aliyun DirectMail. EnvId: {}", response.getEnvId());
            
        } catch (ClientException e) {
            log.error("❌ Aliyun DirectMail error: Code={}, Message={}", e.getErrCode(), e.getErrMsg());
            throw new RuntimeException("Failed to send email via DirectMail: " + e.getErrMsg(), e);
        }
    }
    
    // 📱 Real SMS (placeholder for Aliyun SMS service)
    private void sendRealSMS(NotificationLog notificationLog) {
        // TODO: Implement Aliyun SMS service
        log.warn("📱 SMS service not yet implemented. Would send to: {} - Message: {}", 
                notificationLog.getRecipient(), notificationLog.getMessage());
    }
    
    // 🎨 Professional Email Template
    private String createEmailHtmlBody(NotificationLog notificationLog) {
        return String.format("""
            <!DOCTYPE html>
            <html>
            <head>
                <meta charset="UTF-8">
                <title>SmartQueue Notification</title>
                <style>
                    body { font-family: 'Segoe UI', Arial, sans-serif; line-height: 1.6; color: #333; }
                    .container { max-width: 600px; margin: 0 auto; padding: 20px; }
                    .header { background: linear-gradient(135deg, #667eea 0%%, #764ba2 100%%); color: white; padding: 30px; text-align: center; border-radius: 10px 10px 0 0; }
                    .content { background: #f8f9fa; padding: 30px; border-radius: 0 0 10px 10px; }
                    .highlight { background: #e3f2fd; padding: 15px; border-radius: 8px; margin: 20px 0; border-left: 4px solid #2196F3; }
                    .footer { text-align: center; margin-top: 30px; color: #666; font-size: 14px; }
                    .btn { display: inline-block; padding: 12px 30px; background: #4CAF50; color: white; text-decoration: none; border-radius: 25px; font-weight: bold; }
                </style>
            </head>
            <body>
                <div class="container">
                    <div class="header">
                        <h1>🎯 SmartQueue</h1>
                        <p>Your queue position update</p>
                    </div>
                    <div class="content">
                        <h2>Hello! 👋</h2>
                        <p>We wanted to let you know about your queue status:</p>
                        
                        <div class="highlight">
                            <h3>📝 %s</h3>
                            <p><strong>Notification Time:</strong> %s</p>
                        </div>
                        
                        <p>Thank you for using SmartQueue! We're working hard to serve you as quickly as possible.</p>
                        
                        <p style="text-align: center;">
                            <a href="#" class="btn">Check Queue Status</a>
                        </p>
                    </div>
                    <div class="footer">
                        <p>SmartQueue - Smart waiting, better experience</p>
                        <p>🌐 Multi-cloud powered by AWS & Aliyun</p>
                    </div>
                </div>
            </body>
            </html>
            """, 
            notificationLog.getMessage(),
            Instant.now().toString()
        );
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