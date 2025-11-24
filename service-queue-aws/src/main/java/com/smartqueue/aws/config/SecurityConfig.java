package com.smartqueue.aws.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.server.SecurityWebFilterChain;

@Configuration
@EnableWebFluxSecurity
public class SecurityConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityWebFilterChain securityWebFilterChain(ServerHttpSecurity http) {
        return http
                .csrf().disable()
                .authorizeExchange(exchanges -> exchanges
                        .pathMatchers("/users/register", "/users/login").permitAll()
                        .pathMatchers("/users/**").permitAll() // Allow all user operations for dev
                        .pathMatchers("/queues/**").permitAll() // Queue operations
                        .pathMatchers("/eta").permitAll() // ETA endpoint
                        .pathMatchers("/stats/**").permitAll() // Stats endpoints
                        .pathMatchers("/actuator/**").permitAll() // Health checks and metrics
                        .anyExchange().permitAll() // Allow all for development
                )
                .build();
    }
}