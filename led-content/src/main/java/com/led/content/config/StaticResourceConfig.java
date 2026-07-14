package com.led.content.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.io.File;

/** 静态资源映射 — 上传文件通过 /uploads/ 路径访问 */
@Configuration
public class StaticResourceConfig implements WebMvcConfigurer {

    @Value("${file.upload-dir}")
    private String uploadDir;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        File dir = new File(uploadDir);
        if (!dir.exists()) dir.mkdirs();
        registry.addResourceHandler("/uploads/**")
                .addResourceLocations("file:" + dir.getAbsolutePath() + File.separator);
    }
}
