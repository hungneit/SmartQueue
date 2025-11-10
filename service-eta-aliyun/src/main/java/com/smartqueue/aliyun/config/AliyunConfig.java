package com.smartqueue.aliyun.config;

import com.alicloud.openservices.tablestore.SyncClient;
import com.alicloud.openservices.tablestore.ClientConfiguration;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;

@Configuration
@Slf4j
public class AliyunConfig {
    
    @Value("${aliyun.access-key:demo-key}")
    private String accessKeyId;
    
    @Value("${aliyun.access-secret:demo-secret}")
    private String accessKeySecret;
    
    @Value("${aliyun.tablestore.endpoint:https://demo.ap-southeast-1.ots.aliyuncs.com}")
    private String tablestoreEndpoint;
    
    @Value("${aliyun.tablestore.instance:demo-instance}")
    private String tablestoreInstance;
    
    @Value("${aliyun.region:ap-southeast-1}")
    private String region;
    
    @Value("${spring.profiles.active:dev}")
    private String activeProfile;
    
    // 🔥 Real TableStore Client (for production)
    @Bean(name = "syncClient")
    @ConditionalOnProperty(name = "aliyun.tablestore.enabled", havingValue = "true", matchIfMissing = false)
    public SyncClient tableStoreClient() {
        if (accessKeyId.equals("demo-key") || accessKeySecret.equals("demo-secret") || 
            tablestoreEndpoint.contains("demo")) {
            log.warn("TableStore configuration using demo values, skipping real client creation");
            return null;
        }
        
        try {
            log.info("Initializing REAL TableStore client for instance: {}", tablestoreInstance);
            ClientConfiguration clientConfiguration = new ClientConfiguration();
            clientConfiguration.setConnectionTimeoutInMillisecond(5000);
            clientConfiguration.setSocketTimeoutInMillisecond(5000);
            
            return new SyncClient(tablestoreEndpoint, accessKeyId, accessKeySecret, tablestoreInstance, clientConfiguration);
        } catch (Exception e) {
            log.error("Failed to create TableStore client", e);
            return null;
        }
    }
    
    // 🧪 No TableStore Client in development mode
    // The repository will handle null client gracefully
    
    // 📧 DirectMail Configuration (handled directly in NotificationService)
    @Bean
    public boolean directMailEnabled() {
        boolean enabled = !accessKeyId.equals("demo-key") && !accessKeySecret.equals("demo-secret");
        log.info("DirectMail enabled: {} (using demo credentials: {})", enabled, !enabled);
        return enabled;
    }
    
    // Configuration beans
    @Bean
    public String etaStatsTableName() {
        return "smartqueue_eta_stats";
    }
    
    @Bean
    public String accessKeyId() {
        return accessKeyId;
    }
    
    @Bean
    public String accessKeySecret() {
        return accessKeySecret;
    }
    
    @Bean
    public String region() {
        return region;
    }
}