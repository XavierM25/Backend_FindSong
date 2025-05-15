package com.findsong.findsongapi.service;

import com.findsong.findsongapi.dto.AudioRecognitionResponseDto;
import com.findsong.findsongapi.dto.ConsolidatedSongResponseDto;
import com.findsong.findsongapi.dto.SpotifyArtistDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class AudioRecognitionService {

    private final ShazamService shazamService;
    private final SpotifyService spotifyService;

    public ConsolidatedSongResponseDto identifySong(byte[] audioData) {
        try {
            log.info("Iniciando identificación de canción con {} bytes de audio", audioData.length);
            AudioRecognitionResponseDto shazamResponse = shazamService.identifySong(audioData);

            if (!shazamResponse.isSuccess()) {
                return ConsolidatedSongResponseDto.builder()
                        .success(false)
                        .message(shazamResponse.getMessage())
                        .build();
            }

            // Obtener información de Spotify
            SpotifyArtistDto spotifyInfo = spotifyService.getArtistInfo(shazamResponse.getSong().getArtist());
            
            return ConsolidatedSongResponseDto.builder()
                    .success(true)
                    .message("Canción identificada correctamente")
                    .shazamInfo(shazamResponse.getSong())
                    .spotifyInfo(spotifyInfo)
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