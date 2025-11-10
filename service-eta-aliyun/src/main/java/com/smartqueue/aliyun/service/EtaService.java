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

import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
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
        log.info("Calculating SMART ETA for queueId: {}, ticketId: {}, position: {}", queueId, ticketId, position);
        
        try {
            // Get current ETA stats - use mock repository in dev profile
            Optional<EtaStats> statsOpt;
            if (mockEtaStatsRepository != null) {
                statsOpt = mockEtaStatsRepository.findLatestByQueueId(queueId);
            } else {
                statsOpt = etaStatsRepository.findLatestByQueueId(queueId);
            }
            
            double baseServiceRate = defaultServiceRate;
            int p90Wait = 10;
            int p50Wait = 5;
            
            if (statsOpt.isPresent()) {
                EtaStats stats = statsOpt.get();
                baseServiceRate = stats.getEmaServiceRate();
                p90Wait = stats.getP90WaitTimeMinutes();
                p50Wait = stats.getP50WaitTimeMinutes();
            }
            
            // 🧠 SMART ETA CALCULATION với Time-based Factors
            LocalDateTime now = LocalDateTime.now(ZoneId.systemDefault());
            double smartServiceRate = calculateSmartServiceRate(baseServiceRate, now);
            
            // Calculate base ETA with smart service rate
            double baseEtaMinutes = position / smartServiceRate;
            
            // Apply additional factors
            double finalEta = applySmartFactors(baseEtaMinutes, now, position);
            int estimatedWaitMinutes = Math.max(1, (int) Math.ceil(finalEta));
            
            log.info("SMART ETA calculated - Queue: {}, Position: {}, Base: {:.1f}min, Smart: {}min, Factors: Peak={}, Lunch={}, Weekend={}", 
                    queueId, position, baseEtaMinutes, estimatedWaitMinutes, 
                    isPeakHour(now), isLunchTime(now), isWeekend(now));
            
            return EtaResponse.builder()
                    .queueId(queueId)
                    .ticketId(ticketId)
                    .estimatedWaitMinutes(estimatedWaitMinutes)
                    .p90WaitMinutes(p90Wait)
                    .p50WaitMinutes(p50Wait)
                    .serviceRate(smartServiceRate)
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
    
    // 🧠 SMART SERVICE RATE CALCULATION với Time-based Factors
    private double calculateSmartServiceRate(double baseRate, LocalDateTime now) {
        double multiplier = 1.0;
        
        // Peak Hours: 9-11AM và 2-4PM (slower service)
        if (isPeakHour(now)) {
            multiplier *= 0.7;  // 30% slower
        }
        
        // Lunch Time: 12-1PM (much slower)
        if (isLunchTime(now)) {
            multiplier *= 0.5;  // 50% slower
        }
        
        // Weekend: Generally slower
        if (isWeekend(now)) {
            multiplier *= 0.8;  // 20% slower
        }
        
        // Late hours: 6-8PM (slightly faster as people leave)
        if (isEveningRush(now)) {
            multiplier *= 1.2;  // 20% faster
        }
        
        return Math.max(0.1, baseRate * multiplier); // Minimum service rate
    }
    
    // 🎯 SMART FACTORS APPLICATION
    private double applySmartFactors(double baseEta, LocalDateTime now, int position) {
        double eta = baseEta;
        
        // Position-based factor: Higher positions get slightly longer estimates (buffer)
        if (position > 10) {
            eta *= 1.1;  // 10% buffer for longer queues
        }
        
        // Day-of-week factors
        DayOfWeek dayOfWeek = now.getDayOfWeek();
        switch (dayOfWeek) {
            case MONDAY:
                eta *= 1.15;  // Mondays are typically slower
                break;
            case FRIDAY:
                eta *= 1.1;   // Fridays slightly slower
                break;
            case SATURDAY:
            case SUNDAY:
                eta *= 0.9;   // Weekends often faster (fewer people)
                break;
        }
        
        // Time-based uncertainty factor (add small buffer for reliability)
        eta *= 1.05;  // 5% reliability buffer
        
        return eta;
    }
    
    // 🕐 TIME DETECTION HELPERS
    private boolean isPeakHour(LocalDateTime now) {
        LocalTime time = now.toLocalTime();
        return (time.isAfter(LocalTime.of(9, 0)) && time.isBefore(LocalTime.of(11, 0))) ||
               (time.isAfter(LocalTime.of(14, 0)) && time.isBefore(LocalTime.of(16, 0)));
    }
    
    private boolean isLunchTime(LocalDateTime now) {
        LocalTime time = now.toLocalTime();
        return time.isAfter(LocalTime.of(12, 0)) && time.isBefore(LocalTime.of(13, 30));
    }
    
    private boolean isWeekend(LocalDateTime now) {
        DayOfWeek dayOfWeek = now.getDayOfWeek();
        return dayOfWeek == DayOfWeek.SATURDAY || dayOfWeek == DayOfWeek.SUNDAY;
    }
    
    private boolean isEveningRush(LocalDateTime now) {
        LocalTime time = now.toLocalTime();
        return time.isAfter(LocalTime.of(18, 0)) && time.isBefore(LocalTime.of(20, 0));
    }
}