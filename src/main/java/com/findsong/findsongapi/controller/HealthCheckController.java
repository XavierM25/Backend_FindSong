package com.findsong.findsongapi.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.actuate.health.Health;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class HealthCheckController {

    @Value("${spring.application.name:FindSongAPI}")
    private String applicationName;

    private final LocalDateTime startTime = LocalDateTime.now();

    @GetMapping
    public Map<String, Object> getHealthCheck() {
        Map<String, Object> response = new HashMap<>();
        response.put("status", "ok");
        response.put("service", applicationName);
        response.put("message", "Servicio de reconocimiento de audio funcionando correctamente");
        response.put("timestamp", LocalDateTime.now().format(DateTimeFormatter.ISO_DATE_TIME));
        return response;
    }

    @GetMapping("/health")
    public Health getDetailedHealth() {
        LocalDateTime now = LocalDateTime.now();
        long uptimeSeconds = java.time.Duration.between(startTime, now).getSeconds();

        return Health.up()
                .withDetail("service", applicationName)
                .withDetail("uptime", formatUptime(uptimeSeconds))
                .withDetail("startTime", startTime.format(DateTimeFormatter.ISO_DATE_TIME))
                .withDetail("timestamp", now.format(DateTimeFormatter.ISO_DATE_TIME))
                .withDetail("javaVersion", System.getProperty("java.version"))
                .build();
    }

    private String formatUptime(long seconds) {
        long days = seconds / 86400;
        seconds %= 86400;
        long hours = seconds / 3600;
        seconds %= 3600;
        long minutes = seconds / 60;
        seconds %= 60;

        return String.format("%dd %dh %dm %ds", days, hours, minutes, seconds);
    }
}