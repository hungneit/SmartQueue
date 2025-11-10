package com.smartqueue.aliyun.service;

import com.smartqueue.aliyun.dto.response.EtaResponse;
import com.smartqueue.aliyun.model.EtaStats;
import com.smartqueue.aliyun.repository.EtaStatsRepository;
import com.smartqueue.aliyun.repository.MockEtaStatsRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Optional;

@Slf4j
@Service
public class EtaService {
    
    @Autowired(required = false)
    private EtaStatsRepository etaStatsRepository;
    
    @Autowired(required = false)
    private MockEtaStatsRepository mockEtaStatsRepository;
    
    @Value("${eta.calculation.ema-alpha:0.3}")
    private double emaAlpha;
    
    @Value("${eta.calculation.default-service-rate:1.0}")
    private double defaultServiceRate;
    
    public EtaResponse calculateEta(String queueId, String ticketId, Integer position) {
        log.info("Calculating ETA for queueId: {}, ticketId: {}, position: {}", queueId, ticketId, position);
        
        try {
            // Get current ETA stats - use mock repository in dev profile
            Optional<EtaStats> statsOpt;
            if (mockEtaStatsRepository != null) {
                statsOpt = mockEtaStatsRepository.findLatestByQueueId(queueId);
            } else {
                statsOpt = etaStatsRepository.findLatestByQueueId(queueId);
            }
            
            double serviceRate = defaultServiceRate;
            int p90Wait = 10;
            int p50Wait = 5;
            
            if (statsOpt.isPresent()) {
                EtaStats stats = statsOpt.get();
                serviceRate = stats.getEmaServiceRate();
                p90Wait = stats.getP90WaitTimeMinutes();
                p50Wait = stats.getP50WaitTimeMinutes();
            }
            
            // Calculate ETA: position / service_rate (customers per minute)
            int estimatedWaitMinutes = Math.max(1, (int) Math.ceil(position / serviceRate));
            
            log.info("ETA calculated - Queue: {}, Position: {}, Wait: {} minutes", queueId, position, estimatedWaitMinutes);
            
            return EtaResponse.builder()
                    .queueId(queueId)
                    .ticketId(ticketId)
                    .estimatedWaitMinutes(estimatedWaitMinutes)
                    .p90WaitMinutes(p90Wait)
                    .p50WaitMinutes(p50Wait)
                    .serviceRate(serviceRate)
                    .updatedAt(Instant.now())
                    .build();
                    
        } catch (Exception e) {
            log.error("Error calculating ETA for queue: {}", queueId, e);
            
            // Return fallback ETA
            return EtaResponse.builder()
                    .queueId(queueId)
                    .ticketId(ticketId)
                    .estimatedWaitMinutes(position != null ? position * 5 : 10)
                    .p90WaitMinutes(10)
                    .p50WaitMinutes(5)
                    .serviceRate(defaultServiceRate)
                    .updatedAt(Instant.now())
                    .build();
        }
    }
    
    public void updateServiceStats(String queueId, int servedCount, int windowSec) {
        log.info("Updating service stats for queueId: {}, served: {}, window: {}sec", queueId, servedCount, windowSec);
        
        try {
            // Calculate service rate (customers per minute)
            double serviceRate = (double) servedCount / (windowSec / 60.0);
            
            // Update EMA service rate - use mock repository in dev profile
            if (mockEtaStatsRepository != null) {
                mockEtaStatsRepository.updateServiceRate(queueId, serviceRate, emaAlpha);
            } else {
                etaStatsRepository.updateServiceRate(queueId, serviceRate, emaAlpha);
            }
            
            log.info("Service stats updated successfully for queueId: {}", queueId);
            
        } catch (Exception e) {
            log.error("Error updating service stats for queue: {}", queueId, e);
            throw new RuntimeException("Failed to update service stats", e);
        }
    }
    
    public EtaStats getLatestStats(String queueId) {
        Optional<EtaStats> statsOpt;
        if (mockEtaStatsRepository != null) {
            statsOpt = mockEtaStatsRepository.findLatestByQueueId(queueId);
        } else {
            statsOpt = etaStatsRepository.findLatestByQueueId(queueId);
        }
        
        return statsOpt
                .orElse(EtaStats.builder()
                        .queueId(queueId)
                        .emaServiceRate(defaultServiceRate)
                        .p90WaitTimeMinutes(10)
                        .p50WaitTimeMinutes(5)
                        .servedCount(0)
                        .updatedAt(Instant.now())
                        .build());
    }
}