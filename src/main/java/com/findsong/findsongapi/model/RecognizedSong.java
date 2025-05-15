package com.findsong.findsongapi.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RecognizedSong {
    private String title;
    private String artist;
    private String album;
    private String releaseDate;
    private Integer duration;
    private List<String> genres;
    private String spotifyId;
    private String appleMusicId;
    private String coverArtUrl;
    private String previewUrl;
}