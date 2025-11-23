package com.smartqueue.aws.controller;

import com.smartqueue.aws.dto.request.BulkJoinRequest;
import com.smartqueue.aws.dto.request.CreateQueueRequest;
import com.smartqueue.aws.dto.request.UpdateQueueRequest;
import com.smartqueue.aws.dto.request.JoinQueueRequest;
import com.smartqueue.aws.dto.request.ProcessNextRequest;
import com.smartqueue.aws.dto.response.JoinQueueResponse;
import com.smartqueue.aws.dto.response.ProcessNextResponse;
import com.smartqueue.aws.dto.response.QueueStatusResponse;
import com.smartqueue.aws.service.QueueService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/queues")
@RequiredArgsConstructor
@Validated
public class QueueController {
    
    private final QueueService queueService;
    
    @Value("${test.api-key}")
    private String testApiKey;
    
    @PostMapping("/{queueId}/join")
    public ResponseEntity<JoinQueueResponse> joinQueue(
            @PathVariable @NotBlank String queueId,
            @RequestBody @Valid JoinQueueRequest request) {
        
        log.info("Join queue request received for queueId: {}", queueId);
        
        try {
            JoinQueueResponse response = queueService.joinQueue(queueId, request);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error in join queue", e);
            return ResponseEntity.badRequest().body(
                JoinQueueResponse.builder()
                    .queueId(queueId)
                    .message("Failed to join queue: " + e.getMessage())
                    .build()
            );
        }
    }

    @GetMapping
    public ResponseEntity<List<Map<String, Object>>> getAllQueues() {
        log.info("Get all queues request received");
        try {
            List<com.smartqueue.aws.model.QueueInfo> queues = queueService.getAllQueues();
            
            // Convert to simple response format
            List<Map<String, Object>> response = new ArrayList<>();
            for (com.smartqueue.aws.model.QueueInfo queue : queues) {
                Map<String, Object> queueMap = new java.util.HashMap<>();
                queueMap.put("queueId", queue.getQueueId());
                queueMap.put("queueName", queue.getQueueName());
                queueMap.put("isActive", queue.getIsActive());
                queueMap.put("openSlots", queue.getOpenSlots());
                queueMap.put("maxCapacity", queue.getMaxCapacity());
                response.add(queueMap);
            }
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error getting all queues", e);
            return ResponseEntity.internalServerError().build();
        }
    }
    
    @PostMapping
    public ResponseEntity<?> createQueue(@RequestBody @Valid CreateQueueRequest request) {
        log.info("Create queue request received: {}", request.getQueueId());
        
        try {
            com.smartqueue.aws.model.QueueInfo queue = queueService.createQueue(request);
            return ResponseEntity.ok(Map.of(
                "message", "Queue created successfully",
                "queue", queue
            ));
        } catch (Exception e) {
            log.error("Error creating queue", e);
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
    
    @GetMapping("/{queueId}")
    public ResponseEntity<?> getQueueDetail(@PathVariable @NotBlank String queueId) {
        log.info("Get queue detail request for: {}", queueId);
        
        try {
            com.smartqueue.aws.model.QueueInfo queue = queueService.getQueueById(queueId);
            return ResponseEntity.ok(queue);
        } catch (Exception e) {
            log.error("Error getting queue detail", e);
            return ResponseEntity.notFound().build();
        }
    }
    
    @PutMapping("/{queueId}")
    public ResponseEntity<?> updateQueue(
            @PathVariable @NotBlank String queueId,
            @RequestBody @Valid UpdateQueueRequest request) {
        
        log.info("Update queue request received for: {}", queueId);
        
        try {
            com.smartqueue.aws.model.QueueInfo queue = queueService.updateQueue(queueId, request);
            return ResponseEntity.ok(Map.of(
                "message", "Queue updated successfully",
                "queue", queue
            ));
        } catch (Exception e) {
            log.error("Error updating queue", e);
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
    
    @DeleteMapping("/{queueId}")
    public ResponseEntity<?> deleteQueue(@PathVariable @NotBlank String queueId) {
        log.info("Delete queue request received for: {}", queueId);
        
        try {
            queueService.deleteQueue(queueId);
            return ResponseEntity.ok(Map.of(
                "message", "Queue deleted successfully",
                "queueId", queueId
            ));
        } catch (Exception e) {
            log.error("Error deleting queue", e);
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
    
    @GetMapping("/{queueId}/status")
    public ResponseEntity<QueueStatusResponse> getStatus(
            @PathVariable @NotBlank String queueId,
            @RequestParam @NotBlank String ticketId) {
        
        log.info("Status request received for queueId: {}, ticketId: {}", queueId, ticketId);
        
        try {
            QueueStatusResponse response = queueService.getQueueStatus(queueId, ticketId);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error getting queue status", e);
            return ResponseEntity.badRequest().body(
                QueueStatusResponse.builder()
                    .queueId(queueId)
                    .ticketId(ticketId)
                    .message("Failed to get status: " + e.getMessage())
                    .build()
            );
        }
    }
    
    @PostMapping("/{queueId}/next")
    public ResponseEntity<ProcessNextResponse> processNext(
            @PathVariable @NotBlank String queueId,
            @RequestBody @Valid ProcessNextRequest request) {
        
        log.info("Process next request received for queueId: {}, count: {}", queueId, request.getCount());
        
        try {
            ProcessNextResponse response = queueService.processNext(queueId, request);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error processing next", e);
            return ResponseEntity.badRequest().body(
                ProcessNextResponse.builder()
                    .queueId(queueId)
                    .message("Failed to process next: " + e.getMessage())
                    .build()
            );
        }
    }
    
    // Test endpoint for load testing
    @PostMapping("/test/join-bulk")
    public ResponseEntity<?> joinBulk(
            @RequestHeader(value = "X-Test-Key", required = false) String testKey,
            @RequestBody @Valid BulkJoinRequest request) {
        
        log.info("Bulk join request received for queueId: {}, batch: {}", request.getQueueId(), request.getBatch());
        
        // Validate test key
        if (testKey == null || !testKey.equals(testApiKey)) {
            return ResponseEntity.status(403).body(Map.of("error", "Invalid test key"));
        }
        
        try {
            List<JoinQueueResponse> responses = new ArrayList<>();
            
            for (int i = 0; i < request.getBatch(); i++) {
                JoinQueueRequest joinRequest = JoinQueueRequest.builder()
                        .userId("test-user-" + i)
                        .build();
                
                JoinQueueResponse response = queueService.joinQueue(request.getQueueId(), joinRequest);
                responses.add(response);
            }
            
            return ResponseEntity.ok(Map.of(
                "message", "Bulk join completed",
                "processed", request.getBatch(),
                "responses", responses
            ));
            
        } catch (Exception e) {
            log.error("Error in bulk join", e);
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
}