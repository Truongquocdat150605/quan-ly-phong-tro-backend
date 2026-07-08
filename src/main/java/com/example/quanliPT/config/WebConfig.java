package com.example.quanliPT.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Paths;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // ✅ Dùng đường dẫn tương đối ./uploads/ so với thư mục gốc dự án
        String uploadPath = Paths.get("uploads").toAbsolutePath().toUri().toString();

        registry.addResourceHandler("/uploads/**")
                .addResourceLocations(uploadPath + "/");

        System.out.println("📁 [WebConfig] Serving uploads from: " + uploadPath);
    }
}
