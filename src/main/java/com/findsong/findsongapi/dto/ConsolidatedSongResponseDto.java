package com.findsong.findsongapi.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConsolidatedSongResponseDto {
    private boolean success;
    private String message;
    private SongResponseDto shazamInfo;
    private SpotifyArtistDto spotifyInfo;
} 