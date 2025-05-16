package com.findsong.findsongapi.controller;

import com.findsong.findsongapi.dto.ConsolidatedSongResponseDto;
import com.findsong.findsongapi.dto.IdentifySongDto;
import com.findsong.findsongapi.exception.BadRequestException;
import com.findsong.findsongapi.service.AudioRecognitionService;
import com.findsong.findsongapi.util.FileStorageUtil;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

@RestController
@RequestMapping("/api/audio-recognition")
@RequiredArgsConstructor
@Slf4j
public class AudioRecognitionController {

    private final AudioRecognitionService audioRecognitionService;
    private final FileStorageUtil fileStorageUtil;

    private static final int MAX_PROCESSED_SIZE = 500000;

    @PostMapping(value = "/identify", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ConsolidatedSongResponseDto> identifySong(
            @RequestParam("audio") MultipartFile file,
            @Valid IdentifySongDto identifySongDto) {

        if (file.isEmpty()) {
            throw new BadRequestException("No se ha enviado ningún archivo de audio");
        }

        log.info("Procesando archivo de tamaño: {} bytes", file.getSize());
        Path tempFilePath = null;

        try {
            tempFilePath = fileStorageUtil.storeFile(file);
            byte[] audioData = readAndLimitAudioData(tempFilePath, file.getSize());

            ConsolidatedSongResponseDto result = audioRecognitionService.identifySong(audioData);

            return ResponseEntity.ok(result);
        } catch (IOException e) {
            log.error("Error al identificar canción: {}", e.getMessage());
            return ResponseEntity.ok(
                    ConsolidatedSongResponseDto.builder()
                            .success(false)
                            .message("Error al procesar la solicitud: " + e.getMessage())
                            .build());
        } finally {
            if (tempFilePath != null) {
                fileStorageUtil.deleteFile(tempFilePath);
            }
        }
    }

    private byte[] readAndLimitAudioData(Path filePath, long originalSize) throws IOException {
        byte[] audioData = Files.readAllBytes(filePath);

        if (originalSize > MAX_PROCESSED_SIZE) {
            log.info("Archivo demasiado grande, limitando a {} bytes", MAX_PROCESSED_SIZE);
            byte[] trimmedAudio = new byte[MAX_PROCESSED_SIZE];
            System.arraycopy(audioData, 0, trimmedAudio, 0, MAX_PROCESSED_SIZE);
            return trimmedAudio;
        }

        return audioData;
    }
}