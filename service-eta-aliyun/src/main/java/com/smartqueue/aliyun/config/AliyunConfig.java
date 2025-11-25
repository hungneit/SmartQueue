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

    @Value("${aliyun.access-key}")
    private String accessKeyId;

    @Value("${aliyun.access-secret}")
    private String accessKeySecret;

    @Value("${aliyun.tablestore.endpoint}")
    private String tablestoreEndpoint;

    @Value("${aliyun.tablestore.instance}")
    private String tablestoreInstance;

    @Value("${aliyun.region}")
    private String region;

    @Value("${spring.profiles.active:dev}")
    private String activeProfile;

    // 🔥 TableStore Client
    @Bean(name = "syncClient")
    @ConditionalOnProperty(name = "aliyun.tablestore.enabled", havingValue = "true")
    public SyncClient tableStoreClient() {

        // ➜ Không cần check demo nữa (đã bỏ default demo rồi)
        log.info("Initializing REAL TableStore client for instance: {}", tablestoreInstance);

        try {
            ClientConfiguration clientConfiguration = new ClientConfiguration();
            clientConfiguration.setConnectionTimeoutInMillisecond(5000);
            clientConfiguration.setSocketTimeoutInMillisecond(5000);

            return new SyncClient(
                tablestoreEndpoint,
                accessKeyId,
                accessKeySecret,
                tablestoreInstance,
                clientConfiguration
            );

        } catch (Exception e) {
            log.error("Failed to create TableStore client", e);
            return null;
        }
    }

    // DirectMail
    @Bean
    public boolean directMailEnabled() {
        return true;
    }

    @Bean
    public String etaStatsTableName() {
        return "smartq_eta_stats";
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
