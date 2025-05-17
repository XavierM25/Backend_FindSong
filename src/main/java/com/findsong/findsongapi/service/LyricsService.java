package com.findsong.findsongapi.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.findsong.findsongapi.dto.LyricsResponseDto;
import lombok.extern.slf4j.Slf4j;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

@Service
@Slf4j
public class LyricsService {

    private final ObjectMapper objectMapper;
    private static final String LYRICS_API_BASE_URL = "https://api.lyrics.ovh/v1";
    private final OkHttpClient client;

    public LyricsService(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
        this.client = new OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .build();
    }

    @Cacheable(value = "lyrics", key = "#artist + '-' + #title")
    public LyricsResponseDto getLyrics(String artist, String title) {
        try {
            String encodedArtist = java.net.URLEncoder.encode(artist, "UTF-8");
            String encodedTitle = java.net.URLEncoder.encode(title, "UTF-8");
            String url = String.format("%s/%s/%s", LYRICS_API_BASE_URL, encodedArtist, encodedTitle);

            Request request = new Request.Builder()
                    .url(url)
                    .get()
                    .build();

            try (Response response = client.newCall(request).execute()) {
                if (!response.isSuccessful()) {
                    log.warn("Error obteniendo letras para {} - {}: {}", artist, title, response.code());
                    return LyricsResponseDto.builder()
                            .error("No se encontraron letras para esta canción")
                            .build();
                }

                JsonNode jsonResponse = objectMapper.readTree(response.body().string());
                String lyrics = jsonResponse.get("lyrics").asText();

                if (lyrics == null || lyrics.trim().isEmpty()) {
                    return LyricsResponseDto.builder()
                            .error("No se encontraron letras para esta canción")
                            .build();
                }

                return LyricsResponseDto.builder()
                        .lyrics(lyrics)
                        .build();
            }
        } catch (IOException e) {
            log.error("Error al obtener letras para {} - {}: {}", artist, title, e.getMessage());
            return LyricsResponseDto.builder()
                    .error("Error al obtener las letras: " + e.getMessage())
                    .build();
        }
    }
}