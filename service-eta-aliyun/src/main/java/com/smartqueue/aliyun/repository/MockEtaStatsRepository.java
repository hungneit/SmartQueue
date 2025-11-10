package com.smartqueue.aliyun.repository;

import com.smartqueue.aliyun.model.EtaStats;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Slf4j
@Repository
@Profile("dev") // Chỉ sử dụng trong development
public class MockEtaStatsRepository {
    
    // In-memory storage for development
    private final ConcurrentMap<String, EtaStats> etaStatsMap = new ConcurrentHashMap<>();
    
    public EtaStats save(EtaStats etaStats) {
        log.debug("Saving ETA stats for queue: {}", etaStats.getQueueId());
        
        etaStats.setUpdatedAt(Instant.now());
        String key = etaStats.getQueueId() + "#" + etaStats.getTimeWindow();
        etaStatsMap.put(key, etaStats);
        
        log.info("ETA stats saved successfully for queue: {}", etaStats.getQueueId());
        return etaStats;
    }
    
    public Optional<EtaStats> findByQueueIdAndTimeWindow(String queueId, String timeWindow) {
        log.debug("Finding ETA stats for queue: {} and time window: {}", queueId, timeWindow);
        
        String key = queueId + "#" + timeWindow;
        return Optional.ofNullable(etaStatsMap.get(key));
    }
    
    public Optional<EtaStats> findLatestByQueueId(String queueId) {
        log.debug("Finding latest ETA stats for queue: {}", queueId);
        
        String currentTimeWindow = getCurrentTimeWindow();
        return findByQueueIdAndTimeWindow(queueId, currentTimeWindow);
    }
    
    public EtaStats updateServiceRate(String queueId, double newServiceRate, double alpha) {
        log.debug("Updating service rate for queue: {} to {} with alpha: {}", queueId, newServiceRate, alpha);
        
        String timeWindow = getCurrentTimeWindow();
        Optional<EtaStats> existingStats = findByQueueIdAndTimeWindow(queueId, timeWindow);
        
        EtaStats etaStats;
        if (existingStats.isPresent()) {
            etaStats = existingStats.get();
            // EMA calculation: new_ema = alpha * new_value + (1 - alpha) * old_ema
            double oldEma = etaStats.getEmaServiceRate();
            double newEma = alpha * newServiceRate + (1 - alpha) * oldEma;
            etaStats.setEmaServiceRate(newEma);
            etaStats.setServedCount(etaStats.getServedCount() + 1);
        } else {
            etaStats = EtaStats.builder()
                    .queueId(queueId)
                    .timeWindow(timeWindow)
                    .windowStart(Instant.now())
                    .servedCount(1)
                    .emaServiceRate(newServiceRate)
                    .p90WaitTimeMinutes(5) // Default values
                    .p50WaitTimeMinutes(3)
                    .build();
        }
        
        return save(etaStats);
    }
    
    public void deleteByQueueId(String queueId) {
        log.debug("Deleting ETA stats for queue: {}", queueId);
        
        String currentTimeWindow = getCurrentTimeWindow();
        String key = queueId + "#" + currentTimeWindow;
        etaStatsMap.remove(key);
        
        log.info("ETA stats deleted successfully for queue: {}", queueId);
    }
    
    private String getCurrentTimeWindow() {
        return DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH").format(Instant.now().atZone(java.time.ZoneOffset.UTC));
    }
}