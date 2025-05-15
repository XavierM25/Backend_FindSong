package com.findsong.findsongapi.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SpotifyArtistDto {
    private String id;
    private String name;
    private String bio;
    private Integer followers;
    private List<String> genres;
    private String spotifyUrl;
    private List<SocialMediaDto> socialMedia;
    private List<AlbumDto> albums;
    private List<ImageDto> images;
} 