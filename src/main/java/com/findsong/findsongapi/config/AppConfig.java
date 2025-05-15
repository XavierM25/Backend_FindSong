package com.findsong.findsongapi.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Getter
@Setter
@ConfigurationProperties(prefix = "app")
@Configuration
public class AppConfig {
    private String rapidApiKey;
    private String corsOrigins;

    private Http http = new Http();
    private Shazam shazam = new Shazam();

    @Getter
    @Setter
    public static class Http {
        private int connectTimeout = 30000;
        private int readTimeout = 30000;
        private int writeTimeout = 30000;
    }

    @Getter
    @Setter
    public static class Shazam {
        private String apiHost = "shazam-song-recognition-api.p.rapidapi.com";
        private String endpoint = "/recognize/file";
        private int maxAudioSize = 500000;
    }
}