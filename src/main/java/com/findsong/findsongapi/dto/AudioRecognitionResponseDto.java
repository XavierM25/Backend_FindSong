package com.findsong.findsongapi.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AudioRecognitionResponseDto {
    private boolean success;
    private SongResponseDto song;
    private String message;
}