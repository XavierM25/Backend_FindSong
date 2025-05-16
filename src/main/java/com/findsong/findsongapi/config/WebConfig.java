package com.findsong.findsongapi.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
@RequiredArgsConstructor
public class WebConfig implements WebMvcConfigurer {

    private final ObjectMapper objectMapper;
    private final PerformanceInterceptor performanceInterceptor;

    @Bean
    public ResponseTransformerInterceptor responseTransformerInterceptor() {
        return new ResponseTransformerInterceptor(objectMapper);
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // Registrar el interceptor de rendimiento primero para capturar tiempos
        // completos
        registry.addInterceptor(performanceInterceptor);

        // Luego registrar el interceptor de transformación de respuestas
        registry.addInterceptor(responseTransformerInterceptor());
    }
}