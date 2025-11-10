package com.smartqueue.aliyun.config;

import com.alicloud.openservices.tablestore.SyncClient;
import com.alicloud.openservices.tablestore.ClientConfiguration;
import com.aliyun.mns.client.CloudAccount;
import com.aliyun.mns.client.MNSClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AliyunConfig {
    
    @Value("${aliyun.region}")
    private String region;
    
    @Value("${aliyun.access-key-id}")
    private String accessKeyId;
    
    @Value("${aliyun.access-key-secret}")
    private String accessKeySecret;
    
    @Value("${aliyun.ots.endpoint}")
    private String otsEndpoint;
    
    @Value("${aliyun.ots.instance}")
    private String otsInstanceName;
    
    @Value("${aliyun.mns.endpoint}")
    private String mnsEndpoint;
    
    @Value("${aliyun.mns.queue-name}")
    private String notificationQueueName;
    
    @Bean
    public SyncClient tableStoreClient() {
        // For development, return a mock client if endpoint is not configured
        if (otsEndpoint == null || otsEndpoint.isEmpty() || otsEndpoint.startsWith("http://localhost")) {
            // Return a mock implementation for development
            return null; // This should be replaced with a proper mock in development
        }
        
        ClientConfiguration clientConfiguration = new ClientConfiguration();
        clientConfiguration.setConnectionTimeoutInMillisecond(5000);
        clientConfiguration.setSocketTimeoutInMillisecond(5000);
        // Remove retry strategy as DefaultRetryStrategy class doesn't exist in current version
        
        return new SyncClient(otsEndpoint, accessKeyId, accessKeySecret, otsInstanceName, clientConfiguration);
    }
    
    @Bean
    public MNSClient mnsClient() {
        // For development, return a mock client if endpoint is not configured
        if (mnsEndpoint == null || mnsEndpoint.isEmpty() || mnsEndpoint.startsWith("http://localhost")) {
            // Return a mock implementation for development
            return null; // This should be replaced with a proper mock in development
        }
        
        CloudAccount account = new CloudAccount(accessKeyId, accessKeySecret, mnsEndpoint);
        return account.getMNSClient();
    }
    
    @Bean
    public String etaStatsTableName() {
        return "smartqueue_eta_stats";
    }
    
    @Bean
    public String notificationQueueName() {
        return notificationQueueName;
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