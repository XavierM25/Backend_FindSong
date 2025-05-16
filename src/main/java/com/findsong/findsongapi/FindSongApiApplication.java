package com.findsong.findsongapi;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;

@SpringBootApplication
@EnableConfigurationProperties
@EnableScheduling
@EnableMongoRepositories
public class FindSongApiApplication {

    public static void main(String[] args) {
        SpringApplication.run(FindSongApiApplication.class, args);
    }
}