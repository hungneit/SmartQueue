package com.smartqueue.aws.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateQueueRequest {
    
    @NotBlank(message = "Queue ID is required")
    private String queueId;
    
    @NotBlank(message = "Queue name is required")
    private String queueName;
    
    @Min(value = 1, message = "Max capacity must be at least 1")
    private Integer maxCapacity;
    
    @Min(value = 0, message = "Open slots must be at least 0")
    private Integer openSlots;
    
    private Boolean isActive = true;
}
