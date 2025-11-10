package com.smartqueue.aliyun.repository;

import com.alicloud.openservices.tablestore.SyncClient;
import com.alicloud.openservices.tablestore.model.*;
import com.smartqueue.aliyun.model.EtaStats;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.Optional;

@Slf4j
@Repository
public class EtaStatsRepository {
    
    private final SyncClient tableStoreClient;
    private final String etaStatsTableName;
    
    public EtaStatsRepository(@Autowired(required = false) SyncClient tableStoreClient, 
                             String etaStatsTableName) {
        this.tableStoreClient = tableStoreClient;
        this.etaStatsTableName = etaStatsTableName;
        log.info("EtaStatsRepository initialized with client: {}", tableStoreClient != null ? "REAL" : "NULL");
    }
    
    public EtaStats save(EtaStats etaStats) {
        log.debug("Saving ETA stats for queue: {}", etaStats.getQueueId());
        
        if (tableStoreClient == null) {
            log.warn("TableStore client is null, skipping save operation");
            etaStats.setUpdatedAt(Instant.now());
            return etaStats;
        }
        
        try {
            etaStats.setUpdatedAt(Instant.now());
            
            PrimaryKeyBuilder primaryKeyBuilder = PrimaryKeyBuilder.createPrimaryKeyBuilder();
            primaryKeyBuilder.addPrimaryKeyColumn("queueId", PrimaryKeyValue.fromString(etaStats.getQueueId()));
            primaryKeyBuilder.addPrimaryKeyColumn("timeWindow", PrimaryKeyValue.fromString(etaStats.getTimeWindow()));
            
            RowPutChange rowPutChange = new RowPutChange(etaStatsTableName, primaryKeyBuilder.build());
            
            // Add attributes
            rowPutChange.addColumn(new Column("servedCount", ColumnValue.fromLong(etaStats.getServedCount())));
            rowPutChange.addColumn(new Column("emaServiceRate", ColumnValue.fromDouble(etaStats.getEmaServiceRate())));
            rowPutChange.addColumn(new Column("p90WaitTimeMinutes", ColumnValue.fromLong(etaStats.getP90WaitTimeMinutes())));
            rowPutChange.addColumn(new Column("p50WaitTimeMinutes", ColumnValue.fromLong(etaStats.getP50WaitTimeMinutes())));
            rowPutChange.addColumn(new Column("windowStart", ColumnValue.fromLong(etaStats.getWindowStart().toEpochMilli())));
            rowPutChange.addColumn(new Column("updatedAt", ColumnValue.fromLong(etaStats.getUpdatedAt().toEpochMilli())));
            
            PutRowRequest request = new PutRowRequest(rowPutChange);
            tableStoreClient.putRow(request);
            
            log.info("ETA stats saved successfully for queue: {}", etaStats.getQueueId());
            return etaStats;
            
        } catch (Exception e) {
            log.error("Error saving ETA stats for queue: {}", etaStats.getQueueId(), e);
            throw new RuntimeException("Failed to save ETA stats", e);
        }
    }
    
    public Optional<EtaStats> findByQueueIdAndTimeWindow(String queueId, String timeWindow) {
        log.debug("Finding ETA stats for queue: {} and time window: {}", queueId, timeWindow);
        
        if (tableStoreClient == null) {
            log.warn("TableStore client is null, returning empty result");
            return Optional.empty();
        }
        
        try {
            PrimaryKeyBuilder primaryKeyBuilder = PrimaryKeyBuilder.createPrimaryKeyBuilder();
            primaryKeyBuilder.addPrimaryKeyColumn("queueId", PrimaryKeyValue.fromString(queueId));
            primaryKeyBuilder.addPrimaryKeyColumn("timeWindow", PrimaryKeyValue.fromString(timeWindow));
            
            SingleRowQueryCriteria criteria = new SingleRowQueryCriteria(etaStatsTableName, primaryKeyBuilder.build());
            criteria.setMaxVersions(1);
            
            GetRowRequest request = new GetRowRequest(criteria);
            GetRowResponse response = tableStoreClient.getRow(request);
            
            Row row = response.getRow();
            if (row != null && !row.isEmpty()) {
                return Optional.of(convertRowToEtaStats(row, queueId, timeWindow));
            }
            
            return Optional.empty();
            
        } catch (Exception e) {
            log.error("Error finding ETA stats for queue: {} and time window: {}", queueId, timeWindow, e);
            return Optional.empty();
        }
    }
    
    public Optional<EtaStats> findLatestByQueueId(String queueId) {
        log.debug("Finding latest ETA stats for queue: {}", queueId);
        
        // For simplicity, use current hour as time window
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
        
        if (tableStoreClient == null) {
            log.warn("TableStore client is null, skipping delete operation");
            return;
        }
        
        try {
            // Note: This is a simplified deletion for the current time window
            // In production, you might want to delete multiple time windows
            String currentTimeWindow = getCurrentTimeWindow();
            
            PrimaryKeyBuilder primaryKeyBuilder = PrimaryKeyBuilder.createPrimaryKeyBuilder();
            primaryKeyBuilder.addPrimaryKeyColumn("queueId", PrimaryKeyValue.fromString(queueId));
            primaryKeyBuilder.addPrimaryKeyColumn("timeWindow", PrimaryKeyValue.fromString(currentTimeWindow));
            
            RowDeleteChange rowDeleteChange = new RowDeleteChange(etaStatsTableName, primaryKeyBuilder.build());
            DeleteRowRequest request = new DeleteRowRequest(rowDeleteChange);
            
            tableStoreClient.deleteRow(request);
            
            log.info("ETA stats deleted successfully for queue: {}", queueId);
            
        } catch (Exception e) {
            log.error("Error deleting ETA stats for queue: {}", queueId, e);
            throw new RuntimeException("Failed to delete ETA stats", e);
        }
    }
    
    private EtaStats convertRowToEtaStats(Row row, String queueId, String timeWindow) {
        return EtaStats.builder()
                .queueId(queueId)
                .timeWindow(timeWindow)
                .servedCount((int) row.getLatestColumn("servedCount").getValue().asLong())
                .emaServiceRate(row.getLatestColumn("emaServiceRate").getValue().asDouble())
                .p90WaitTimeMinutes((int) row.getLatestColumn("p90WaitTimeMinutes").getValue().asLong())
                .p50WaitTimeMinutes((int) row.getLatestColumn("p50WaitTimeMinutes").getValue().asLong())
                .windowStart(Instant.ofEpochMilli(row.getLatestColumn("windowStart").getValue().asLong()))
                .updatedAt(Instant.ofEpochMilli(row.getLatestColumn("updatedAt").getValue().asLong()))
                .build();
    }
    
    private String getCurrentTimeWindow() {
        // Use hour-based time window (e.g., "2024-11-10T14")
        return DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH").format(Instant.now().atZone(java.time.ZoneOffset.UTC));
    }
}