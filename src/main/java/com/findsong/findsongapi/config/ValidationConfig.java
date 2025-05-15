package com.findsong.findsongapi.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class ValidationConfig implements WebMvcConfigurer {

    private final AppConfig appConfig;

    public ValidationConfig(AppConfig appConfig) {
        this.appConfig = appConfig;
    }

    @Bean
    public LocalValidatorFactoryBean validator() {
        return new LocalValidatorFactoryBean();
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        String corsOrigins = appConfig.getCorsOrigins();

        registry.addMapping("/api/**")
                .allowedOriginPatterns(corsOrigins != null ? corsOrigins.split(",") : new String[]{"*"})
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("Origin", "Content-Type", "Accept", "Authorization")
                .allowCredentials(true)
                .maxAge(3600);
    }
}