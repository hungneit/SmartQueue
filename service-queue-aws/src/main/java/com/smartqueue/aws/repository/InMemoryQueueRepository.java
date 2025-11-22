package com.smartqueue.aws.repository;

import com.smartqueue.aws.model.QueueInfo;
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
public class InMemoryQueueRepository extends QueueRepository {
    
    private final Map<String, QueueInfo> queueStore = new ConcurrentHashMap<>();

    public InMemoryQueueRepository() {
        super(null, null);
        log.info("🧪 InMemoryQueueRepository initialized for development mode");
        initializeDefaultQueues();
    }

    private void initializeDefaultQueues() {
        // Create some default queues for testing
        QueueInfo queue1 = QueueInfo.builder()
            .queueId("hospital-queue-1")
            .queueName("General Consultation")
            .openSlots(100)
            .maxCapacity(100)
            .isActive(true)
            .serviceRateEma(0.5)
            .createdAt(Instant.now())
            .updatedAt(Instant.now())
            .build();
        queueStore.put(queue1.getQueueId(), queue1);
        
        QueueInfo queue2 = QueueInfo.builder()
            .queueId("hospital-queue-2")
            .queueName("Emergency Room")
            .openSlots(50)
            .maxCapacity(50)
            .isActive(true)
            .serviceRateEma(0.6)
            .createdAt(Instant.now())
            .updatedAt(Instant.now())
            .build();
        queueStore.put(queue2.getQueueId(), queue2);
        
        log.info("✅ [IN-MEMORY] Initialized {} default queues", queueStore.size());
    }

    @Override
    public QueueInfo save(QueueInfo queueInfo) {
        log.debug("💾 [IN-MEMORY] Saving queue: {}", queueInfo.getQueueId());
        
        queueInfo.setUpdatedAt(Instant.now());
        if (queueInfo.getCreatedAt() == null) {
            queueInfo.setCreatedAt(Instant.now());
        }
        
        queueStore.put(queueInfo.getQueueId(), queueInfo);
        log.info("✅ [IN-MEMORY] Queue saved: {}", queueInfo.getQueueId());
        return queueInfo;
    }

    @Override
    public Optional<QueueInfo> findById(String queueId) {
        log.debug("🔍 [IN-MEMORY] Finding queue by ID: {}", queueId);
        QueueInfo queue = queueStore.get(queueId);
        return Optional.ofNullable(queue);
    }

    public void delete(String queueId) {
        log.debug("🗑️  [IN-MEMORY] Deleting queue: {}", queueId);
        queueStore.remove(queueId);
    }

    public List<QueueInfo> findAll() {
        log.debug("📋 [IN-MEMORY] Finding all queues");
        return new ArrayList<>(queueStore.values());
    }

    public List<QueueInfo> findActiveQueues() {
        log.debug("📋 [IN-MEMORY] Finding active queues");
        return queueStore.values().stream()
                .filter(q -> q.getIsActive() != null && q.getIsActive())
                .collect(Collectors.toList());
    }
    
    public void clear() {
        queueStore.clear();
        log.info("🧹 [IN-MEMORY] Queue store cleared");
    }
}
