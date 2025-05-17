package com.findsong.findsongapi.service;

import com.findsong.findsongapi.dto.AudioRecognitionResponseDto;
import com.findsong.findsongapi.dto.ConsolidatedSongResponseDto;
import com.findsong.findsongapi.dto.SpotifyArtistDto;
import com.findsong.findsongapi.exception.BadRequestException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class AudioRecognitionService {

    private final ShazamService shazamService;
    private final SpotifyService spotifyService;
    private final LyricsService lyricsService;

    public ConsolidatedSongResponseDto identifySong(byte[] audioData) {
        if (audioData == null || audioData.length == 0) {
            log.warn("Se recibió un archivo de audio vacío o nulo");
            throw new BadRequestException("El archivo de audio proporcionado está vacío o es inválido");
        }

        try {
            log.info("Iniciando identificación de canción con {} bytes de audio", audioData.length);
            AudioRecognitionResponseDto shazamResponse = shazamService.identifySong(audioData);

            if (!shazamResponse.isSuccess()) {
                log.warn("No se pudo identificar la canción: {}", shazamResponse.getMessage());
                return ConsolidatedSongResponseDto.builder()
                        .success(false)
                        .message(shazamResponse.getMessage())
                        .build();
            }

            log.info("Canción identificada correctamente: {} - {}",
                    shazamResponse.getSong().getTitle(),
                    shazamResponse.getSong().getArtist());

            // Obtener información de Spotify
            SpotifyArtistDto spotifyInfo = null;
            try {
                spotifyInfo = spotifyService.getArtistInfo(
                        shazamResponse.getSong().getArtist(),
                        shazamResponse.getSong().getTitle());

                log.info("Información de Spotify obtenida para artista: {}",
                        shazamResponse.getSong().getArtist());
            } catch (Exception e) {
                log.warn("No se pudo obtener información de Spotify: {}", e.getMessage());
                // Continuamos aunque no haya información de Spotify
            }

            // Obtener letras de la canción
            String lyrics = null;
            try {
                var lyricsResponse = lyricsService.getLyrics(
                        shazamResponse.getSong().getArtist(),
                        shazamResponse.getSong().getTitle());
                if (lyricsResponse.getLyrics() != null) {
                    lyrics = lyricsResponse.getLyrics();
                    log.info("Letras obtenidas para: {} - {}",
                            shazamResponse.getSong().getTitle(),
                            shazamResponse.getSong().getArtist());
                }
            } catch (Exception e) {
                log.warn("No se pudieron obtener las letras: {}", e.getMessage());
                // Continuamos aunque no haya letras
            }

            return ConsolidatedSongResponseDto.builder()
                    .success(true)
                    .message("Canción identificada correctamente")
                    .shazamInfo(shazamResponse.getSong())
                    .spotifyInfo(spotifyInfo)
                    .lyrics(lyrics)
                    .build();

        } catch (Exception e) {
            log.error("Error en el servicio de reconocimiento de audio: {}", e.getMessage(), e);
            return ConsolidatedSongResponseDto.builder()
                    .success(false)
                    .message("Error en el servicio de reconocimiento: " + e.getMessage())
                    .build();
        }
    }
}