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
public class AlbumDto {
    private String id;
    private String name;
    private String releaseDate;
    private String coverUrl;
    private List<TrackDto> tracks;
} 