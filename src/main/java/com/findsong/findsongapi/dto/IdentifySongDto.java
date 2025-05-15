package com.findsong.findsongapi.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Data;

@Data
public class IdentifySongDto {

    @Min(1)
    @Max(30)
    private Integer duration;
}