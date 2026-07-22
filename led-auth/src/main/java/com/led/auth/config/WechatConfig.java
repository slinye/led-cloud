package com.led.auth.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.web.client.RestTemplate;

import java.util.Arrays;

/** 微信小程序配置 */
@Data
@Configuration
@ConfigurationProperties(prefix = "wechat.miniapp")
public class WechatConfig {
    private String appid;
    private String secret;
    /** 开发模式：开启后绕过微信API，用code直接生成mock openid */
    private boolean mock = false;

    @Bean
    public RestTemplate restTemplate() {
        RestTemplate restTemplate = new RestTemplate();
        // 微信 API 返回 text/plain 的 JSON，需要让 Jackson 也能处理 text/plain
        MappingJackson2HttpMessageConverter converter = new MappingJackson2HttpMessageConverter();
        converter.setSupportedMediaTypes(Arrays.asList(
                MediaType.APPLICATION_JSON,
                MediaType.TEXT_PLAIN,
                new MediaType("application", "*+json")
        ));
        restTemplate.setMessageConverters(Arrays.asList(converter));
        return restTemplate;
    }
}
