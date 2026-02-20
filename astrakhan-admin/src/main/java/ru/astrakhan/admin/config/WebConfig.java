package ru.astrakhan.admin.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/v1/**") // Только API-эндпоинты
                .allowedOrigins(
                        "http://localhost:5173",  // Vue Vite dev-server
                        "http://localhost:3001",  // Node.js Express server
                        "http://127.0.0.1:5173",
                        "http://127.0.0.1:3001"
                )
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .allowCredentials(true)
                .maxAge(3600); // Кэшировать preflight-запросы 1 час
    }
}