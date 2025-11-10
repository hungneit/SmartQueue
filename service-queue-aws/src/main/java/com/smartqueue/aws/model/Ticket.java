package com.smartqueue.aws.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbPartitionKey;

import java.time.Instant;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@DynamoDbBean
public class Ticket {
    
    private String ticketId;
    private String queueId;
    private TicketStatus status;
    private Instant joinedAt;
    private Instant lastNotifiedAt;
    private Integer position;
    private String userEmail;
    private String userPhone;
    
    @DynamoDbPartitionKey
    public String getTicketId() {
        return ticketId;
    }
    
    public void setTicketId(String ticketId) {
        this.ticketId = ticketId;
    }

    public static String generateTicketId() {
        return UUID.randomUUID().toString();
    }
    
    public enum TicketStatus {
        WAITING,
        SERVED,
        CANCELLED,
        EXPIRED
    }
}