package com.led.auth.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

/** 微信小程序配置 */
@Data
@Configuration
@ConfigurationProperties(prefix = "wechat.miniapp")
public class WechatConfig {
    private String appid;
    private String secret;

    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }
}
