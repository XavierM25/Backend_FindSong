package com.findsong.findsongapi.config;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;

import java.util.HashMap;
import java.util.Map;

@RequiredArgsConstructor
@Slf4j
public class ResponseTransformerInterceptor implements HandlerInterceptor {

    private final ObjectMapper objectMapper;
    private static final String API_PATH_PREFIX = "/api";

    @Override
    public void postHandle(
            HttpServletRequest request,
            HttpServletResponse response,
            Object handler,
            ModelAndView modelAndView) {

        if (!request.getRequestURI().startsWith(API_PATH_PREFIX)) {
            return;
        }

        if (response.getStatus() < 200 || response.getStatus() >= 300 ||
                response.getContentType() == null ||
                !response.getContentType().contains("application/json")) {
            return;
        }

        log.debug("Procesando respuesta para: {}", request.getRequestURI());
    }
}