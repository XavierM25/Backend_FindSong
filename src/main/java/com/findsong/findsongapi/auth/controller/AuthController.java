package com.findsong.findsongapi.auth.controller;

import com.findsong.findsongapi.auth.dto.AuthResponseDto;
import com.findsong.findsongapi.auth.dto.LoginRequestDto;
import com.findsong.findsongapi.auth.dto.RegisterRequestDto;
import com.findsong.findsongapi.auth.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Slf4j
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    public ResponseEntity<AuthResponseDto> register(@Valid @RequestBody RegisterRequestDto request) {
        log.info("Solicitud de registro para usuario: {}", request.getUsername());
        AuthResponseDto response = authService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponseDto> login(@Valid @RequestBody LoginRequestDto request) {
        log.info("Intento de login para usuario: {}", request.getUsername());
        AuthResponseDto response = authService.login(request);
        return ResponseEntity.ok(response);
    }
}