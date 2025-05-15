package com.findsong.findsongapi.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.findsong.findsongapi.config.AppConfig;
import com.findsong.findsongapi.dto.AudioRecognitionResponseDto;
import com.findsong.findsongapi.dto.SongResponseDto;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

@Service
@Slf4j
public class ShazamService {

    private final AppConfig appConfig;
    private final ObjectMapper objectMapper;
    private static final int MAX_AUDIO_SIZE = 500000;
    private static final String SHAZAM_API_URL = "https://shazam-song-recognition-api.p.rapidapi.com/recognize/file";
    private static final String CONTENT_TYPE = "application/octet-stream";

    private final OkHttpClient okHttpClient;

    public ShazamService(AppConfig appConfig, ObjectMapper objectMapper) {
        this.appConfig = appConfig;
        this.objectMapper = objectMapper;

        this.okHttpClient = new OkHttpClient.Builder()
                .connectTimeout(appConfig.getHttp().getConnectTimeout(), TimeUnit.MILLISECONDS)
                .readTimeout(appConfig.getHttp().getReadTimeout(), TimeUnit.MILLISECONDS)
                .writeTimeout(appConfig.getHttp().getWriteTimeout(), TimeUnit.MILLISECONDS)
                .build();
    }

    public AudioRecognitionResponseDto identifySong(byte[] audioData) {
        try {
            byte[] processedAudioData = limitAudioSize(audioData);

            RequestBody requestBody = RequestBody.create(MediaType.parse(CONTENT_TYPE), processedAudioData);
            Request request = buildShazamRequest(requestBody);

            log.info("Enviando solicitud a Shazam API...");
            return executeShazamRequest(request);
        } catch (IOException e) {
            log.error("Error identificando canción: {}", e.getMessage(), e);
            return buildErrorResponse("Error en el servicio de reconocimiento: " + e.getMessage());
        }
    }

    private byte[] limitAudioSize(byte[] audioData) {
        if (audioData.length <= MAX_AUDIO_SIZE) {
            return audioData;
        }

        byte[] processedAudioData = new byte[MAX_AUDIO_SIZE];
        System.arraycopy(audioData, 0, processedAudioData, 0, MAX_AUDIO_SIZE);
        log.info("Tamaño de audio reducido de {} bytes a {} bytes", audioData.length, MAX_AUDIO_SIZE);
        return processedAudioData;
    }

    private Request buildShazamRequest(RequestBody requestBody) {
        return new Request.Builder()
                .url(SHAZAM_API_URL)
                .post(requestBody)
                .addHeader("x-rapidapi-key", appConfig.getRapidApiKey())
                .addHeader("x-rapidapi-host", "shazam-song-recognition-api.p.rapidapi.com")
                .addHeader("Content-Type", CONTENT_TYPE)
                .build();
    }

    private AudioRecognitionResponseDto executeShazamRequest(Request request) throws IOException {
        try (Response response = okHttpClient.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                log.error("Error de API: {} - {}", response.code(), response.message());
                return buildErrorResponse("Error al conectar con el servicio de reconocimiento: " + response.message());
            }

            String responseBody = response.body().string();
            log.debug("Respuesta de la API: {}", responseBody);

            JsonNode result = objectMapper.readTree(responseBody);

            if (!result.has("track")) {
                return buildErrorResponse("No se pudo identificar la canción. Intenta con otro fragmento de audio.");
            }

            JsonNode track = result.get("track");
            SongResponseDto song = extractSongInfo(track);

            return AudioRecognitionResponseDto.builder()
                    .success(true)
                    .song(song)
                    .message("Canción identificada correctamente")
                    .build();
        }
    }

    private AudioRecognitionResponseDto buildErrorResponse(String message) {
        return AudioRecognitionResponseDto.builder()
                .success(false)
                .message(message)
                .build();
    }

    private SongResponseDto extractSongInfo(JsonNode track) {
        String title = getJsonTextOrDefault(track, "title", "");
        String artist = getJsonTextOrDefault(track, "subtitle", "");
        String album = extractMetadataField(track, "Album");
        String releaseDate = extractMetadataField(track, "Released");

        List<String> genres = extractGenres(track);
        String coverArtUrl = extractCoverArtUrl(track);
        String previewUrl = extractPreviewUrl(track);
        String spotifyId = extractSpotifyId(track);
        String appleMusicId = extractAppleMusicId(track);

        return SongResponseDto.builder()
                .title(title)
                .artist(artist)
                .album(album)
                .releaseDate(releaseDate)
                .genres(genres)
                .coverArtUrl(coverArtUrl)
                .previewUrl(previewUrl)
                .spotifyId(spotifyId)
                .appleMusicId(appleMusicId)
                .build();
    }

    private String getJsonTextOrDefault(JsonNode node, String field, String defaultValue) {
        return node.has(field) ? node.get(field).asText() : defaultValue;
    }

    private String extractMetadataField(JsonNode track, String fieldTitle) {
        return Optional.ofNullable(track.get("sections"))
                .filter(sections -> sections.isArray() && sections.size() > 0)
                .map(sections -> sections.get(0))
                .filter(section -> section.has("metadata"))
                .map(section -> {
                    JsonNode metadata = section.get("metadata");
                    for (JsonNode item : metadata) {
                        if (item.has("title") && fieldTitle.equals(item.get("title").asText())) {
                            return item.get("text").asText();
                        }
                    }
                    return null;
                })
                .orElse(null);
    }

    private List<String> extractGenres(JsonNode track) {
        List<String> genres = new ArrayList<>();
        if (track.has("genres") && track.get("genres").has("primary")) {
            genres.add(track.get("genres").get("primary").asText());
        }
        return genres;
    }

    private String extractCoverArtUrl(JsonNode track) {
        return Optional.ofNullable(track.get("images"))
                .filter(images -> images.has("coverart"))
                .map(images -> images.get("coverart").asText())
                .orElse(null);
    }

    private String extractPreviewUrl(JsonNode track) {
        return Optional.ofNullable(track.get("hub"))
                .filter(hub -> hub.has("actions"))
                .map(hub -> {
                    JsonNode actions = hub.get("actions");
                    for (JsonNode action : actions) {
                        if (action.has("type") && "uri".equals(action.get("type").asText())) {
                            return action.get("uri").asText();
                        }
                    }
                    return null;
                })
                .orElse(null);
    }

    private String extractSpotifyId(JsonNode track) {
        return Optional.ofNullable(track.get("providers"))
                .filter(JsonNode::isArray)
                .map(providers -> {
                    for (JsonNode provider : providers) {
                        if (provider.has("type") && "SPOTIFY".equals(provider.get("type").asText())) {
                            if (provider.has("actions") && provider.get("actions").isArray()
                                    && provider.get("actions").size() > 0) {
                                return provider.get("actions").get(0).get("uri").asText();
                            }
                        }
                    }
                    return null;
                })
                .orElse(null);
    }

    private String extractAppleMusicId(JsonNode track) {
        return Optional.ofNullable(track.get("hub"))
                .filter(hub -> hub.has("actions"))
                .map(hub -> {
                    JsonNode actions = hub.get("actions");
                    for (JsonNode action : actions) {
                        if (action.has("type") && "applemusicplay".equals(action.get("type").asText())) {
                            return action.get("id").asText();
                        }
                    }
                    return null;
                })
                .orElse(null);
    }
}