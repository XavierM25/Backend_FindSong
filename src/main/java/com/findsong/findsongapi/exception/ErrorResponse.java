package com.findsong.findsongapi.exception;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.Map;

@Data
@Builder
public class ErrorResponse {
    private int statusCode;
    private String message;
    private LocalDateTime timestamp;
    private Map<String, String> errors;
}