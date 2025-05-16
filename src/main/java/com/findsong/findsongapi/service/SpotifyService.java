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
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
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

    public SpotifyArtistDto getArtistInfo(String artistName, String songTitle) {
        try {
            if (accessToken == null) {
                refreshAccessToken();
            }

            String artistId = searchArtist(artistName);
            if (artistId == null) {
                return null;
            }

            SpotifyArtistDto artistInfo = getArtistDetails(artistId);

            // Buscar el álbum específico que contiene la canción
            AlbumDto songAlbum = findAlbumWithSong(artistId, songTitle);
            if (songAlbum != null) {
                // Establecer solo el álbum relevante
                artistInfo.setAlbums(List.of(songAlbum));
            } else {
                // Si no encontramos el álbum específico, entonces obtenemos los álbumes del
                // artista
                List<AlbumDto> albums = getArtistAlbums(artistId);
                artistInfo.setAlbums(albums);
            }

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

            SpotifyArtistDto artistDto = SpotifyArtistDto.builder()
                    .id(artist.get("id").asText())
                    .name(artist.get("name").asText())
                    .followers(artist.get("followers").get("total").asInt())
                    .genres(extractGenres(artist))
                    .spotifyUrl(artist.get("external_urls").get("spotify").asText())
                    .socialMedia(getSocialMediaLinks(artistId))
                    .images(extractImages(artist))
                    .build();

            // Obtener la biografía del artista
            artistDto.setBio(getArtistBio(artistId));

            return artistDto;
        }
    }

    private String getArtistBio(String artistId) {
        try {
            // Intentamos obtener la información biográfica desde múltiples fuentes
            String bio = getArtistBioFromSpotify(artistId);

            if (bio != null && !bio.isEmpty() && !bio.equals("No hay información biográfica disponible")) {
                return bio;
            }

            // Intentar obtener info de géneros como fallback
            StringBuilder bioBuilder = new StringBuilder();
            bioBuilder.append("Artista conocido por sus géneros: ");

            // Obtener datos del artista para los géneros
            Request request = new Request.Builder()
                    .url(spotifyApiUrl + "/artists/" + artistId)
                    .get()
                    .addHeader("Authorization", "Bearer " + accessToken)
                    .build();

            try (Response response = client.newCall(request).execute()) {
                if (!response.isSuccessful()) {
                    return "No hay información biográfica disponible";
                }

                JsonNode artistInfo = objectMapper.readTree(response.body().string());
                if (artistInfo.has("genres") && artistInfo.get("genres").size() > 0) {
                    List<String> genres = new ArrayList<>();
                    artistInfo.get("genres").forEach(genre -> genres.add(genre.asText()));
                    bioBuilder.append(String.join(", ", genres));
                } else {
                    bioBuilder.append("Información no disponible");
                }

                if (artistInfo.has("popularity")) {
                    bioBuilder.append(". Con una popularidad de ")
                            .append(artistInfo.get("popularity").asInt())
                            .append(" sobre 100 en Spotify.");
                }

                return bioBuilder.toString();
            }
        } catch (Exception e) {
            log.error("Error obteniendo biografía del artista: {}", e.getMessage());
            return "No hay información biográfica disponible";
        }
    }

    private String getArtistBioFromSpotify(String artistId) {
        try {
            // Intento obtener la información biográfica directamente de la página de
            // Spotify
            String artistName = getArtistName(artistId);
            if (artistName == null) {
                return null;
            }

            log.info("Buscando biografía para artista: {} (ID: {})", artistName, artistId);

            // Buscar en la página web oficial de Spotify
            Request request = new Request.Builder()
                    .url("https://open.spotify.com/artist/" + artistId)
                    .get()
                    .addHeader("User-Agent",
                            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36")
                    .build();

            try (Response response = client.newCall(request).execute()) {
                if (!response.isSuccessful()) {
                    log.warn("No se pudo acceder a la página web del artista en Spotify");
                    return getArtistBioFallback(artistId);
                }

                // Obtener la página HTML
                String html = response.body().string();

                // Buscar la sección de "About" en el HTML
                int aboutIndex = html.indexOf("\"biography\":{\"text\":\"");
                if (aboutIndex != -1) {
                    // Extraer el texto de la biografía
                    int startIndex = aboutIndex + "\"biography\":{\"text\":\"".length();
                    int endIndex = html.indexOf("\"", startIndex);

                    if (endIndex > startIndex) {
                        String bio = html.substring(startIndex, endIndex);
                        // Reemplazar los caracteres escapados
                        bio = bio.replace("\\n", "\n")
                                .replace("\\\"", "\"")
                                .replace("\\\\", "\\");

                        if (!bio.isEmpty()) {
                            log.info("Biografía encontrada con éxito para {}", artistName);
                            return bio;
                        }
                    }
                }

                // Intento más agresivo a través de otra sección en el HTML
                aboutIndex = html.indexOf("\"biography\":");
                if (aboutIndex != -1) {
                    int bracesLevel = 0;
                    boolean inQuotes = false;
                    boolean escaped = false;
                    StringBuilder bioJson = new StringBuilder();

                    for (int i = aboutIndex; i < html.length(); i++) {
                        char c = html.charAt(i);
                        bioJson.append(c);

                        if (escaped) {
                            escaped = false;
                            continue;
                        }

                        if (c == '\\') {
                            escaped = true;
                            continue;
                        }

                        if (c == '"' && !escaped) {
                            inQuotes = !inQuotes;
                            continue;
                        }

                        if (!inQuotes) {
                            if (c == '{') {
                                bracesLevel++;
                            } else if (c == '}') {
                                bracesLevel--;
                                if (bracesLevel == 0) {
                                    // Hemos encontrado el objeto JSON completo
                                    try {
                                        String bioJsonStr = bioJson.toString();
                                        JsonNode bioNode = objectMapper.readTree(bioJsonStr);
                                        if (bioNode.has("text")) {
                                            return bioNode.get("text").asText();
                                        }
                                    } catch (Exception e) {
                                        log.warn("Error parsing JSON de biografía: {}", e.getMessage());
                                    }
                                    break;
                                }
                            }
                        }
                    }
                }

                log.warn("No se pudo encontrar la biografía en la página HTML para {}", artistName);
            }

            // Si no se pudo obtener por la página, intentamos obtener info adicional para
            // enriquecer
            return getEnhancedArtistBio(artistId);
        } catch (Exception e) {
            log.error("Error al obtener biografía detallada: {}", e.getMessage());
            return getArtistBioFallback(artistId);
        }
    }

    private String getEnhancedArtistBio(String artistId) {
        try {
            String artistName = getArtistName(artistId);
            if (artistName == null) {
                return getArtistBioFallback(artistId);
            }

            // Obtenemos información básica del artista
            Request request = new Request.Builder()
                    .url(spotifyApiUrl + "/artists/" + artistId)
                    .get()
                    .addHeader("Authorization", "Bearer " + accessToken)
                    .build();

            JsonNode artistInfo;
            try (Response response = client.newCall(request).execute()) {
                if (!response.isSuccessful()) {
                    return getArtistBioFallback(artistId);
                }
                artistInfo = objectMapper.readTree(response.body().string());
            }

            // Obtenemos los álbumes top del artista
            List<AlbumDto> albums = getArtistAlbums(artistId);

            // Construimos una biografía detallada
            StringBuilder enhancedBio = new StringBuilder();
            enhancedBio.append(artistName);

            // Añadimos géneros
            if (artistInfo.has("genres") && artistInfo.get("genres").size() > 0) {
                enhancedBio.append(" es un artista destacado en ");
                List<String> genres = new ArrayList<>();
                artistInfo.get("genres").forEach(genre -> genres.add(genre.asText()));

                if (genres.size() == 1) {
                    enhancedBio.append("el género ").append(genres.get(0));
                } else {
                    enhancedBio.append("los géneros ");
                    for (int i = 0; i < genres.size(); i++) {
                        if (i > 0) {
                            if (i == genres.size() - 1) {
                                enhancedBio.append(" y ");
                            } else {
                                enhancedBio.append(", ");
                            }
                        }
                        enhancedBio.append(genres.get(i));
                    }
                }
                enhancedBio.append(". ");
            } else {
                enhancedBio.append(" es un artista reconocido en la industria musical. ");
            }

            // Añadimos información de popularidad y seguidores
            if (artistInfo.has("popularity")) {
                int popularity = artistInfo.get("popularity").asInt();
                if (popularity > 80) {
                    enhancedBio.append("Actualmente es uno de los artistas más populares en Spotify, ");
                } else if (popularity > 60) {
                    enhancedBio.append("Disfruta de una gran popularidad en Spotify, ");
                } else if (popularity > 40) {
                    enhancedBio.append("Mantiene una presencia sólida en Spotify, ");
                } else {
                    enhancedBio.append("Tiene una presencia en Spotify, ");
                }

                enhancedBio.append("con un índice de popularidad de ")
                        .append(popularity)
                        .append(" sobre 100. ");
            }

            if (artistInfo.has("followers") && artistInfo.get("followers").has("total")) {
                int followers = artistInfo.get("followers").get("total").asInt();
                enhancedBio.append("Su música ha captado la atención de ")
                        .append(String.format("%,d", followers))
                        .append(" seguidores en la plataforma. ");
            }

            // Añadimos información sobre álbumes destacados
            if (albums != null && !albums.isEmpty()) {
                enhancedBio.append("\n\nEntre sus trabajos destacados se encuentran ");

                int albumCount = Math.min(albums.size(), 5); // Limitamos a 5 álbumes como máximo
                for (int i = 0; i < albumCount; i++) {
                    AlbumDto album = albums.get(i);
                    if (i > 0) {
                        if (i == albumCount - 1) {
                            enhancedBio.append(" y ");
                        } else {
                            enhancedBio.append(", ");
                        }
                    }

                    enhancedBio.append("\"").append(album.getName()).append("\"");
                    if (album.getReleaseDate() != null && !album.getReleaseDate().isEmpty()) {
                        enhancedBio.append(" (").append(album.getReleaseDate().substring(0, 4)).append(")");
                    }
                }
                enhancedBio.append(". ");

                // Añadimos información sobre canciones populares
                if (!albums.isEmpty() && albums.get(0).getTracks() != null && !albums.get(0).getTracks().isEmpty()) {
                    enhancedBio.append("\n\nEntre sus canciones más conocidas se incluyen ");

                    List<TrackDto> allTracks = new ArrayList<>();
                    for (AlbumDto album : albums) {
                        if (album.getTracks() != null) {
                            allTracks.addAll(album.getTracks());
                        }
                    }

                    int trackCount = Math.min(allTracks.size(), 5); // Limitamos a 5 canciones
                    for (int i = 0; i < trackCount; i++) {
                        TrackDto track = allTracks.get(i);
                        if (i > 0) {
                            if (i == trackCount - 1) {
                                enhancedBio.append(" y ");
                            } else {
                                enhancedBio.append(", ");
                            }
                        }

                        enhancedBio.append("\"").append(track.getName()).append("\"");
                    }
                    enhancedBio.append(".");
                }
            }

            // Mensaje de cierre
            enhancedBio.append("\n\nDescubre más música de ")
                    .append(artistName)
                    .append(" en Spotify.");

            return enhancedBio.toString();
        } catch (Exception e) {
            log.error("Error generando biografía mejorada: {}", e.getMessage());
            return getArtistBioFallback(artistId);
        }
    }

    private String getArtistName(String artistId) {
        try {
            Request request = new Request.Builder()
                    .url(spotifyApiUrl + "/artists/" + artistId)
                    .get()
                    .addHeader("Authorization", "Bearer " + accessToken)
                    .build();

            try (Response response = client.newCall(request).execute()) {
                if (response.isSuccessful()) {
                    JsonNode artistInfo = objectMapper.readTree(response.body().string());
                    if (artistInfo.has("name")) {
                        return artistInfo.get("name").asText();
                    }
                }
            }
            return null;
        } catch (Exception e) {
            log.error("Error obteniendo nombre del artista: {}", e.getMessage());
            return null;
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
                        .spotifyId(track.get("id").asText())
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

    private List<SocialMediaDto> getSocialMediaLinks(String artistId) {
        List<SocialMediaDto> socialMediaLinks = new ArrayList<>();

        try {
            // Simplificamos este método para devolver solo Spotify
            socialMediaLinks.add(SocialMediaDto.builder()
                    .platform("spotify")
                    .url("https://open.spotify.com/artist/" + artistId)
                    .build());

            log.info("Red social agregada: spotify - https://open.spotify.com/artist/{}", artistId);
            return socialMediaLinks;
        } catch (Exception e) {
            log.error("Error obteniendo redes sociales: {}", e.getMessage());
            // Si falla, devolvemos al menos el enlace a Spotify
            return List.of(SocialMediaDto.builder()
                    .platform("spotify")
                    .url("https://open.spotify.com/artist/" + artistId)
                    .build());
        }
    }

    private void extractSocialMediaFromPage(String html, List<SocialMediaDto> socialMediaLinks) {
        // No necesitamos esta funcionalidad ahora
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

    private AlbumDto findAlbumWithSong(String artistId, String songTitle) {
        try {
            // Primero buscamos la canción
            String encodedSongTitle = java.net.URLEncoder.encode(songTitle, "UTF-8");
            String encodedArtistId = java.net.URLEncoder.encode(artistId, "UTF-8");

            Request request = new Request.Builder()
                    .url(spotifyApiUrl + "/search?q=" + encodedSongTitle + "&type=track&limit=50")
                    .get()
                    .addHeader("Authorization", "Bearer " + accessToken)
                    .build();

            try (Response response = client.newCall(request).execute()) {
                if (!response.isSuccessful()) {
                    log.error("Error buscando la canción: {}", response);
                    return null;
                }

                JsonNode searchResult = objectMapper.readTree(response.body().string());

                // Buscar entre los resultados el que coincida con el artista
                if (searchResult.has("tracks") && searchResult.get("tracks").has("items")) {
                    JsonNode tracks = searchResult.get("tracks").get("items");

                    for (JsonNode track : tracks) {
                        // Verificar si esta canción es del artista que buscamos
                        boolean isArtistMatch = false;
                        JsonNode artists = track.get("artists");
                        for (JsonNode artist : artists) {
                            if (artist.get("id").asText().equals(artistId)) {
                                isArtistMatch = true;
                                break;
                            }
                        }

                        if (isArtistMatch) {
                            // Obtener el álbum de esta canción
                            String albumId = track.get("album").get("id").asText();
                            return getAlbumDetails(albumId);
                        }
                    }
                }
            }

            return null;
        } catch (Exception e) {
            log.error("Error buscando el álbum con la canción: {}", e.getMessage());
            return null;
        }
    }

    private AlbumDto getAlbumDetails(String albumId) {
        try {
            Request request = new Request.Builder()
                    .url(spotifyApiUrl + "/albums/" + albumId)
                    .get()
                    .addHeader("Authorization", "Bearer " + accessToken)
                    .build();

            try (Response response = client.newCall(request).execute()) {
                if (!response.isSuccessful()) {
                    throw new IOException("Error obteniendo detalles del álbum: " + response);
                }

                JsonNode album = objectMapper.readTree(response.body().string());

                AlbumDto albumDto = AlbumDto.builder()
                        .id(album.get("id").asText())
                        .name(album.get("name").asText())
                        .releaseDate(album.get("release_date").asText())
                        .coverUrl(
                                album.get("images").size() > 0 ? album.get("images").get(0).get("url").asText() : null)
                        .build();

                // Obtener las canciones del álbum
                albumDto.setTracks(getAlbumTracks(albumId));

                return albumDto;
            }
        } catch (Exception e) {
            log.error("Error obteniendo detalles del álbum: {}", e.getMessage());
            return null;
        }
    }

    private String getArtistBioFallback(String artistId) {
        try {
            // 1. Intento con la API no oficial que contiene datos de biografía
            Request request = new Request.Builder()
                    .url("https://spclient.wg.spotify.com/open-backend-2/v1/artists/" + artistId
                            + "/desktop?format=json")
                    .get()
                    .addHeader("Authorization", "Bearer " + accessToken)
                    .addHeader("app-platform", "WebPlayer")
                    .build();

            try (Response response = client.newCall(request).execute()) {
                if (response.isSuccessful()) {
                    JsonNode artistInfo = objectMapper.readTree(response.body().string());
                    if (artistInfo.has("artist") && artistInfo.get("artist").has("biography") &&
                            artistInfo.get("artist").get("biography").has("text")) {
                        return artistInfo.get("artist").get("biography").get("text").asText();
                    }

                    // Intento alternativo con otro formato
                    if (artistInfo.has("biography") && artistInfo.get("biography").has("text")) {
                        return artistInfo.get("biography").get("text").asText();
                    }
                } else {
                    log.warn("No se pudo acceder a información extendida del artista");
                }
            }

            // 2. Intento con búsqueda por nombre como último recurso
            String artistName = getArtistName(artistId);
            if (artistName != null) {
                log.info("Generando descripción genérica para: {}", artistName);
                // Aquí podríamos implementar una búsqueda más compleja si fuera necesario

                // Por ahora, extraemos información básica de géneros
                Request genreRequest = new Request.Builder()
                        .url(spotifyApiUrl + "/artists/" + artistId)
                        .get()
                        .addHeader("Authorization", "Bearer " + accessToken)
                        .build();

                try (Response response = client.newCall(genreRequest).execute()) {
                    if (response.isSuccessful()) {
                        JsonNode artistInfo = objectMapper.readTree(response.body().string());
                        StringBuilder bio = new StringBuilder();
                        bio.append(artistName).append(" es un artista ");

                        if (artistInfo.has("genres") && artistInfo.get("genres").size() > 0) {
                            bio.append("conocido por sus géneros: ");
                            List<String> genres = new ArrayList<>();
                            artistInfo.get("genres").forEach(genre -> genres.add(genre.asText()));
                            bio.append(String.join(", ", genres)).append(".");
                        } else {
                            bio.append("con una carrera destacada en la música.");
                        }

                        if (artistInfo.has("popularity")) {
                            bio.append(" Actualmente tiene una popularidad de ")
                                    .append(artistInfo.get("popularity").asInt())
                                    .append(" sobre 100 en Spotify.");
                        }

                        if (artistInfo.has("followers") && artistInfo.get("followers").has("total")) {
                            int followers = artistInfo.get("followers").get("total").asInt();
                            bio.append(" Cuenta con ")
                                    .append(String.format("%,d", followers))
                                    .append(" seguidores en la plataforma.");
                        }

                        return bio.toString();
                    }
                }
            }

            return "Información biográfica no disponible para este artista.";
        } catch (Exception e) {
            log.error("Error en métodos alternativos de biografía: {}", e.getMessage());
            return "No se pudo obtener la información biográfica.";
        }
    }
}