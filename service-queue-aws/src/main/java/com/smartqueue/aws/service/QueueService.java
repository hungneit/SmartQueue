package com.smartqueue.aws.service;

import com.smartqueue.aws.dto.request.CreateQueueRequest;
import com.smartqueue.aws.dto.request.UpdateQueueRequest;
import com.smartqueue.aws.dto.request.JoinQueueRequest;
import com.smartqueue.aws.dto.request.ProcessNextRequest;
import com.smartqueue.aws.dto.response.JoinQueueResponse;
import com.smartqueue.aws.dto.response.ProcessNextResponse;
import com.smartqueue.aws.dto.response.QueueStatusResponse;
import com.smartqueue.aws.model.QueueInfo;
import com.smartqueue.aws.model.Ticket;
import com.smartqueue.aws.model.User;
import com.smartqueue.aws.repository.QueueRepository;
import com.smartqueue.aws.repository.TicketRepository;
import com.smartqueue.aws.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class QueueService {
    
    private final TicketRepository ticketRepository;
    private final QueueRepository queueRepository;
    private final UserRepository userRepository;
    private final WebClient etaServiceWebClient;
    private final Duration etaServiceTimeout;
    
    public JoinQueueResponse joinQueue(String queueId, JoinQueueRequest request) {
        log.info("Processing join queue request for queueId: {} by user: {}", queueId, request.getUserId());
        
        try {
            // Get user information
            Optional<User> userOpt = userRepository.findById(request.getUserId());
            if (userOpt.isEmpty()) {
                throw new RuntimeException("User not found: " + request.getUserId());
            }
            
            User user = userOpt.get();
            
            // Create ticket with full user information
            Ticket ticket = Ticket.builder()
                    .ticketId(Ticket.generateTicketId())
                    .queueId(queueId)
                    .status(Ticket.TicketStatus.WAITING)
                    .userId(user.getUserId())
                    .userEmail(user.getEmail())
                    .userPhone(user.getPhone())
                    .userName(user.getName())
                    .position(getNextPosition(queueId))
                    .joinedAt(Instant.now())
                    .emailNotificationEnabled(user.isEmailNotificationEnabled())
                    .smsNotificationEnabled(user.isSmsNotificationEnabled())
                    .build();
            
            // Save ticket
            ticket = ticketRepository.save(ticket);
            
            // Calculate position
            int position = calculatePosition(queueId, ticket.getTicketId());
            ticket.setPosition(position);
            ticketRepository.save(ticket);
            
            log.info("User joined queue successfully. TicketId: {}, Position: {}", ticket.getTicketId(), position);
            
            return JoinQueueResponse.builder()
                    .ticketId(ticket.getTicketId())
                    .queueId(queueId)
                    .position(position)
                    .message("Successfully joined queue")
                    .build();
                    
        } catch (Exception e) {
            log.error("Error joining queue: {}", queueId, e);
            throw new RuntimeException("Failed to join queue", e);
        }
    }
    
    public Mono<QueueStatusResponse> getQueueStatus(String queueId, String ticketId) {
        log.info("Getting queue status for queueId: {}, ticketId: {}", queueId, ticketId);
        
        return Mono.fromCallable(() -> {
            Optional<Ticket> ticketOpt = ticketRepository.findById(ticketId);
            if (ticketOpt.isEmpty()) {
                throw new RuntimeException("Ticket not found: " + ticketId);
            }
            
            Ticket ticket = ticketOpt.get();
            if (!ticket.getQueueId().equals(queueId)) {
                throw new RuntimeException("Ticket does not belong to this queue");
            }
            
            // Calculate current position
            int currentPosition = calculatePosition(queueId, ticketId);
            return new Object[] { ticket, currentPosition };
        })
        .flatMap(data -> {
            Ticket ticket = (Ticket) ((Object[]) data)[0];
            int currentPosition = (int) ((Object[]) data)[1];
            
            // Get ETA from Service B - now reactive
            return getEstimatedWaitTime(queueId, ticketId, currentPosition)
                .map(estimatedWaitMinutes -> QueueStatusResponse.builder()
                    .ticketId(ticketId)
                    .queueId(queueId)
                    .position(currentPosition)
                    .estimatedWaitMinutes(estimatedWaitMinutes)
                    .status(ticket.getStatus().name())
                    .message("Queue status retrieved successfully")
                    .build());
        })
        .onErrorResume(e -> {
            log.error("Error getting queue status", e);
            return Mono.error(new RuntimeException("Failed to get queue status", e));
        });
    }
    
    public ProcessNextResponse processNext(String queueId, ProcessNextRequest request) {
        log.info("Processing next {} customers for queueId: {}", request.getCount(), queueId);
        
        try {
            List<Ticket> waitingTickets = ticketRepository.findWaitingTicketsByQueue(queueId);
            
            int processCount = Math.min(request.getCount(), waitingTickets.size());
            int processed = 0;
            
            for (int i = 0; i < processCount; i++) {
                Ticket ticket = waitingTickets.get(i);
                ticketRepository.updateStatus(ticket.getTicketId(), Ticket.TicketStatus.SERVED);
                processed++;
            }
            
            // Update open slots
            Optional<QueueInfo> queueInfoOpt = queueRepository.findById(queueId);
            int newOpenSlots = queueInfoOpt.map(QueueInfo::getOpenSlots).orElse(0) + processed;
            queueRepository.updateOpenSlots(queueId, newOpenSlots);
            
            // Notify Service B about the processing
            notifyServiceB(queueId, processed);
            
            log.info("Processed {} customers for queueId: {}", processed, queueId);
            
            return ProcessNextResponse.builder()
                    .queueId(queueId)
                    .dequeuedCount(processed)
                    .newOpenSlots(newOpenSlots)
                    .message("Successfully processed " + processed + " customers")
                    .build();
                    
        } catch (Exception e) {
            log.error("Error processing next customers for queue: {}", queueId, e);
            throw new RuntimeException("Failed to process next customers", e);
        }
    }
    
    private int calculatePosition(String queueId, String ticketId) {
        List<Ticket> waitingTickets = ticketRepository.findWaitingTicketsByQueue(queueId);
        
        // Sort by joinedAt timestamp (create mutable copy first)
        List<Ticket> sortedTickets = new ArrayList<>(waitingTickets);
        sortedTickets.sort((t1, t2) -> t1.getJoinedAt().compareTo(t2.getJoinedAt()));
        
        // Find position of current ticket
        for (int i = 0; i < sortedTickets.size(); i++) {
            if (sortedTickets.get(i).getTicketId().equals(ticketId)) {
                return i + 1; // 1-based position
            }
        }
        
        return sortedTickets.size() + 1; // If not found, put at end
    }
    
    private Mono<Integer> getEstimatedWaitTime(String queueId, String ticketId, int position) {
        return etaServiceWebClient
                .get()
                .uri("/eta?queueId={queueId}&ticketId={ticketId}&position={position}", queueId, ticketId, position)
                .retrieve()
                .bodyToMono(Integer.class)
                .timeout(etaServiceTimeout)
                .onErrorResume(e -> {
                    log.warn("Failed to get ETA from service B, using fallback calculation", e);
                    return Mono.just(position * 5); // Fallback: 5 minutes per position
                });
    }
    
    private void notifyServiceB(String queueId, int servedCount) {
        try {
            etaServiceWebClient
                    .post()
                    .uri("/stats/served")
                    .bodyValue(Map.of("queueId", queueId, "count", servedCount, "windowSec", 60))
                    .retrieve()
                    .bodyToMono(String.class)
                    .timeout(etaServiceTimeout)
                    .subscribe(
                        response -> log.debug("Successfully notified service B: {}", response),
                        error -> log.warn("Failed to notify service B", error)
                    );
        } catch (Exception e) {
            log.warn("Error notifying service B", e);
        }
    }

    private int getNextPosition(String queueId) {
        try {
            List<Ticket> waitingTickets = ticketRepository.findByQueueIdAndStatus(queueId, Ticket.TicketStatus.WAITING);
            return waitingTickets.size() + 1;
        } catch (Exception e) {
            log.error("Error calculating next position for queue: {}", queueId, e);
            return 1; // Fallback to position 1
        }
    }

    public List<QueueInfo> getAllQueues() {
        log.info("Getting all queues");
        try {
            return queueRepository.findAll();
        } catch (Exception e) {
            log.error("Error getting all queues", e);
            throw new RuntimeException("Failed to fetch queues: " + e.getMessage());
        }
    }
    
    public QueueInfo createQueue(CreateQueueRequest request) {
        log.info("Creating new queue: {}", request.getQueueId());
        
        // Check if queue already exists
        Optional<QueueInfo> existing = queueRepository.findById(request.getQueueId());
        if (existing.isPresent()) {
            throw new RuntimeException("Queue already exists: " + request.getQueueId());
        }
        
        QueueInfo queue = QueueInfo.builder()
                .queueId(request.getQueueId())
                .queueName(request.getQueueName())
                .maxCapacity(request.getMaxCapacity())
                .openSlots(request.getOpenSlots())
                .isActive(request.getIsActive())
                .serviceRateEma(0.5)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();
        
        return queueRepository.save(queue);
    }
    
    public QueueInfo getQueueById(String queueId) {
        Optional<QueueInfo> queue = queueRepository.findById(queueId);
        if (queue.isEmpty()) {
            throw new RuntimeException("Queue not found: " + queueId);
        }
        return queue.get();
    }
    
    public QueueInfo updateQueue(String queueId, UpdateQueueRequest request) {
        log.info("Updating queue: {}", queueId);
        
        Optional<QueueInfo> queueOpt = queueRepository.findById(queueId);
        if (queueOpt.isEmpty()) {
            throw new RuntimeException("Queue not found: " + queueId);
        }
        
        QueueInfo queue = queueOpt.get();
        
        if (request.getQueueName() != null) {
            queue.setQueueName(request.getQueueName());
        }
        if (request.getMaxCapacity() != null) {
            queue.setMaxCapacity(request.getMaxCapacity());
        }
        if (request.getOpenSlots() != null) {
            queue.setOpenSlots(request.getOpenSlots());
        }
        if (request.getIsActive() != null) {
            queue.setIsActive(request.getIsActive());
        }
        
        queue.setUpdatedAt(Instant.now());
        
        return queueRepository.save(queue);
    }
    
    public void deleteQueue(String queueId) {
        log.info("Deleting queue: {}", queueId);
        
        Optional<QueueInfo> queueOpt = queueRepository.findById(queueId);
        if (queueOpt.isEmpty()) {
            throw new RuntimeException("Queue not found: " + queueId);
        }
        
        // Check if queue has waiting tickets
        List<Ticket> waitingTickets = ticketRepository.findWaitingTicketsByQueue(queueId);
        if (!waitingTickets.isEmpty()) {
            throw new RuntimeException("Cannot delete queue with " + waitingTickets.size() + " waiting customers");
        }
        
        queueRepository.deleteById(queueId);
    }
}