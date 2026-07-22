package com.led.schedule.config;

import feign.Retryer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Feign 全局配置：禁用重试，避免 LoadBalancer 重试导致 MQTT 命令重复下发
 */
@Configuration
public class FeignConfig {

    @Bean
    public Retryer feignRetryer() {
        return Retryer.NEVER_RETRY;
    }
}
