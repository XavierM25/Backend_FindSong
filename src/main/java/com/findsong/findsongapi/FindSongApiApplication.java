package com.findsong.findsongapi;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableConfigurationProperties
@EnableScheduling
public class FindSongApiApplication {

    public static void main(String[] args) {
        SpringApplication.run(FindSongApiApplication.class, args);
    }
}