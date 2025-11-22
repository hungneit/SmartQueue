package com.smartqueue.aws.repository;

import com.smartqueue.aws.model.Ticket;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * In-memory implementation for development/testing
 */
@Repository
@Primary
@ConditionalOnProperty(name = "app.use-in-memory", havingValue = "true")
@Slf4j
public class InMemoryTicketRepository extends TicketRepository {
    
    private final Map<String, Ticket> ticketStore = new ConcurrentHashMap<>();
    private final Map<String, List<String>> queueTicketsIndex = new ConcurrentHashMap<>();

    public InMemoryTicketRepository() {
        super(null, null);
        log.info("🧪 InMemoryTicketRepository initialized for development mode");
    }

    @Override
    public Ticket save(Ticket ticket) {
        log.debug("💾 [IN-MEMORY] Saving ticket: {}", ticket.getTicketId());
        
        ticket.setUpdatedAt(Instant.now());
        if (ticket.getJoinedAt() == null) {
            ticket.setJoinedAt(Instant.now());
        }
        
        ticketStore.put(ticket.getTicketId(), ticket);
        
        // Update queue index
        queueTicketsIndex.computeIfAbsent(ticket.getQueueId(), k -> new ArrayList<>())
                .add(ticket.getTicketId());
        
        log.info("✅ [IN-MEMORY] Ticket saved: {} (Queue: {}, Position: {})", 
                ticket.getTicketId(), ticket.getQueueId(), ticket.getPosition());
        return ticket;
    }

    @Override
    public Optional<Ticket> findById(String ticketId) {
        log.debug("🔍 [IN-MEMORY] Finding ticket by ID: {}", ticketId);
        Ticket ticket = ticketStore.get(ticketId);
        return Optional.ofNullable(ticket);
    }

    public void delete(String ticketId) {
        log.debug("🗑️  [IN-MEMORY] Deleting ticket: {}", ticketId);
        Ticket ticket = ticketStore.remove(ticketId);
        if (ticket != null) {
            List<String> queueTickets = queueTicketsIndex.get(ticket.getQueueId());
            if (queueTickets != null) {
                queueTickets.remove(ticketId);
            }
        }
    }

    public List<Ticket> findByQueueId(String queueId) {
        log.debug("📋 [IN-MEMORY] Finding tickets for queue: {}", queueId);
        List<String> ticketIds = queueTicketsIndex.getOrDefault(queueId, Collections.emptyList());
        return ticketIds.stream()
                .map(ticketStore::get)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    public List<Ticket> findActiveTicketsByQueueId(String queueId) {
        log.debug("📋 [IN-MEMORY] Finding active tickets for queue: {}", queueId);
        return findByQueueId(queueId).stream()
                .filter(ticket -> "WAITING".equals(ticket.getStatus()) || "CALLED".equals(ticket.getStatus()))
                .sorted(Comparator.comparing(Ticket::getPosition))
                .collect(Collectors.toList());
    }

    public Optional<Ticket> findByQueueIdAndTicketId(String queueId, String ticketId) {
        log.debug("🔍 [IN-MEMORY] Finding ticket: {} in queue: {}", ticketId, queueId);
        Ticket ticket = ticketStore.get(ticketId);
        if (ticket != null && ticket.getQueueId().equals(queueId)) {
            return Optional.of(ticket);
        }
        return Optional.empty();
    }

    @Override
    public List<Ticket> findByQueueIdAndStatus(String queueId, Ticket.TicketStatus status) {
        log.debug("📋 [IN-MEMORY] Finding tickets for queue: {} with status: {}", queueId, status);
        return findByQueueId(queueId).stream()
                .filter(ticket -> ticket.getStatus() == status)
                .collect(Collectors.toList());
    }

    @Override
    public List<Ticket> findWaitingTicketsByQueue(String queueId) {
        log.debug("📋 [IN-MEMORY] Finding waiting tickets for queue: {}", queueId);
        return findByQueueId(queueId).stream()
                .filter(ticket -> ticket.getStatus() == Ticket.TicketStatus.WAITING)
                .collect(Collectors.toList());
    }

    public int countActiveTickets(String queueId) {
        return (int) findByQueueId(queueId).stream()
                .filter(ticket -> "WAITING".equals(ticket.getStatus()) || "CALLED".equals(ticket.getStatus()))
                .count();
    }
    
    public void clear() {
        ticketStore.clear();
        queueTicketsIndex.clear();
        log.info("🧹 [IN-MEMORY] Ticket store cleared");
    }
}
