package com.findsong.findsongapi.service;

import com.findsong.findsongapi.dto.AudioRecognitionResponseDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class AudioRecognitionService {

    private final ShazamService shazamService;

    public AudioRecognitionResponseDto identifySong(byte[] audioData) {
        try {
            log.info("Iniciando identificación de canción con {} bytes de audio", audioData.length);
            AudioRecognitionResponseDto response = shazamService.identifySong(audioData);

            if (response.isSuccess()) {
                log.info("Canción identificada correctamente: {}",
                        response.getSong() != null ? response.getSong().getTitle() : "No disponible");
            } else {
                log.warn("No se pudo identificar la canción: {}", response.getMessage());
            }

            return response;
        } catch (Exception e) {
            log.error("Error en el servicio de reconocimiento de audio: {}", e.getMessage(), e);
            return AudioRecognitionResponseDto.builder()
                    .success(false)
                    .message("Error en el servicio de reconocimiento: " + e.getMessage())
                    .build();
        }
    }
}