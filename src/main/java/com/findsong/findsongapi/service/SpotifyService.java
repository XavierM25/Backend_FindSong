package com.findsong.findsongapi.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.findsong.findsongapi.dto.AlbumDto;
import com.findsong.findsongapi.dto.ImageDto;
import com.findsong.findsongapi.dto.SocialMediaDto;
import com.findsong.findsongapi.dto.SpotifyArtistDto;
import com.findsong.findsongapi.dto.TrackDto;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Service
@Slf4j
public class SpotifyService {

    private final OkHttpClient client;
    private final ObjectMapper objectMapper;
    private final String spotifyApiUrl = "https://api.spotify.com/v1";
    private String accessToken;

    @Value("${app.spotify.client-id}")
    private String clientId;

    @Value("${app.spotify.client-secret}")
    private String clientSecret;

    public SpotifyService(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
        this.client = new OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .build();
    }

    public SpotifyArtistDto getArtistInfo(String artistName) {
        try {
            if (accessToken == null) {
                refreshAccessToken();
            }

            String artistId = searchArtist(artistName);
            if (artistId == null) {
                return null;
            }

            SpotifyArtistDto artistInfo = getArtistDetails(artistId);
            List<AlbumDto> albums = getArtistAlbums(artistId);
            artistInfo.setAlbums(albums);

            return artistInfo;
        } catch (Exception e) {
            log.error("Error obteniendo información del artista: {}", e.getMessage(), e);
            return null;
        }
    }

    private void refreshAccessToken() throws IOException {
        String credentials = clientId + ":" + clientSecret;
        String encodedCredentials = java.util.Base64.getEncoder().encodeToString(credentials.getBytes());

        RequestBody body = new FormBody.Builder()
                .add("grant_type", "client_credentials")
                .build();

        Request request = new Request.Builder()
                .url("https://accounts.spotify.com/api/token")
                .post(body)
                .addHeader("Authorization", "Basic " + encodedCredentials)
                .build();

        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("Error obteniendo token de acceso: " + response);
            }

            JsonNode jsonResponse = objectMapper.readTree(response.body().string());
            accessToken = jsonResponse.get("access_token").asText();
        }
    }

    private String searchArtist(String artistName) throws IOException {
        Request request = new Request.Builder()
                .url(spotifyApiUrl + "/search?q=" + artistName + "&type=artist&limit=1")
                .get()
                .addHeader("Authorization", "Bearer " + accessToken)
                .build();

        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("Error buscando artista: " + response);
            }

            JsonNode jsonResponse = objectMapper.readTree(response.body().string());
            JsonNode artists = jsonResponse.get("artists").get("items");
            
            if (artists.size() > 0) {
                return artists.get(0).get("id").asText();
            }
            return null;
        }
    }

    private SpotifyArtistDto getArtistDetails(String artistId) throws IOException {
        Request request = new Request.Builder()
                .url(spotifyApiUrl + "/artists/" + artistId)
                .get()
                .addHeader("Authorization", "Bearer " + accessToken)
                .build();

        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("Error obteniendo detalles del artista: " + response);
            }

            JsonNode artist = objectMapper.readTree(response.body().string());
            
            return SpotifyArtistDto.builder()
                    .id(artist.get("id").asText())
                    .name(artist.get("name").asText())
                    .followers(artist.get("followers").get("total").asInt())
                    .genres(extractGenres(artist))
                    .spotifyUrl(artist.get("external_urls").get("spotify").asText())
                    .socialMedia(extractSocialMedia(artist))
                    .images(extractImages(artist))
                    .build();
        }
    }

    private List<AlbumDto> getArtistAlbums(String artistId) throws IOException {
        Request request = new Request.Builder()
                .url(spotifyApiUrl + "/artists/" + artistId + "/albums?limit=10")
                .get()
                .addHeader("Authorization", "Bearer " + accessToken)
                .build();

        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("Error obteniendo álbumes: " + response);
            }

            JsonNode jsonResponse = objectMapper.readTree(response.body().string());
            List<AlbumDto> albums = new ArrayList<>();

            for (JsonNode album : jsonResponse.get("items")) {
                AlbumDto albumDto = AlbumDto.builder()
                        .id(album.get("id").asText())
                        .name(album.get("name").asText())
                        .releaseDate(album.get("release_date").asText())
                        .coverUrl(album.get("images").get(0).get("url").asText())
                        .build();

                albumDto.setTracks(getAlbumTracks(album.get("id").asText()));
                albums.add(albumDto);
            }

            return albums;
        }
    }

    private List<TrackDto> getAlbumTracks(String albumId) throws IOException {
        Request request = new Request.Builder()
                .url(spotifyApiUrl + "/albums/" + albumId + "/tracks")
                .get()
                .addHeader("Authorization", "Bearer " + accessToken)
                .build();

        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("Error obteniendo tracks del álbum: " + response);
            }

            JsonNode jsonResponse = objectMapper.readTree(response.body().string());
            List<TrackDto> tracks = new ArrayList<>();

            for (JsonNode track : jsonResponse.get("items")) {
                tracks.add(TrackDto.builder()
                        .id(track.get("id").asText())
                        .name(track.get("name").asText())
                        .duration(track.get("duration_ms").asInt())
                        .previewUrl(track.has("preview_url") ? track.get("preview_url").asText() : null)
                        .build());
            }

            return tracks;
        }
    }

    private List<String> extractGenres(JsonNode artist) {
        List<String> genres = new ArrayList<>();
        artist.get("genres").forEach(genre -> genres.add(genre.asText()));
        return genres;
    }

    private List<SocialMediaDto> extractSocialMedia(JsonNode artist) {
        List<SocialMediaDto> socialMedia = new ArrayList<>();
        JsonNode externalUrls = artist.get("external_urls");
        
        if (externalUrls.has("spotify")) {
            socialMedia.add(SocialMediaDto.builder()
                    .platform("spotify")
                    .url(externalUrls.get("spotify").asText())
                    .build());
        }

        return socialMedia;
    }

    private List<ImageDto> extractImages(JsonNode artist) {
        List<ImageDto> images = new ArrayList<>();
        if (artist.has("images")) {
            artist.get("images").forEach(image -> {
                images.add(ImageDto.builder()
                        .url(image.get("url").asText())
                        .height(image.has("height") ? image.get("height").asInt() : null)
                        .width(image.has("width") ? image.get("width").asInt() : null)
                        .build());
            });
        }
        return images;
    }
} 