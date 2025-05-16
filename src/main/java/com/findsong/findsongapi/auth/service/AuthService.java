package com.findsong.findsongapi.auth.service;

import java.util.List;
import com.findsong.findsongapi.auth.dto.AuthResponseDto;
import com.findsong.findsongapi.auth.dto.LoginRequestDto;
import com.findsong.findsongapi.auth.dto.RegisterRequestDto;
import com.findsong.findsongapi.auth.model.User;
import com.findsong.findsongapi.auth.repository.UserRepository;
import com.findsong.findsongapi.auth.security.JwtUtils;
import com.findsong.findsongapi.exception.BadRequestException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class AuthService implements UserDetailsService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtils jwtUtils;
    private AuthenticationManager authenticationManager;

    public AuthService(UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            JwtUtils jwtUtils) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtils = jwtUtils;
    }

    public void setAuthenticationManager(AuthenticationManager authenticationManager) {
        this.authenticationManager = authenticationManager;
    }

    public AuthResponseDto register(RegisterRequestDto request) {
        // Validar que el usuario no exista
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new BadRequestException("El nombre de usuario ya está en uso");
        }

        // Validar que el email no exista
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new BadRequestException("El correo electrónico ya está en uso");
        }

        User user = User.builder()
                .username(request.getUsername())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .roles(List.of("ROLE_USER"))
                .build();

        userRepository.save(user);
        log.info("Usuario registrado: {}", user.getUsername());

        String token = jwtUtils.generateToken(user);
        return AuthResponseDto.builder()
                .token(token)
                .username(user.getUsername())
                .build();
    }

    public AuthResponseDto login(LoginRequestDto request) {
        if (authenticationManager == null) {
            log.error("AuthenticationManager no inicializado");
            throw new IllegalStateException("AuthenticationManager no ha sido inicializado");
        }

        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword()));

            User user = userRepository.findByUsername(request.getUsername())
                    .orElseThrow(() -> new UsernameNotFoundException("Usuario no encontrado"));

            log.info("Usuario autenticado: {}", user.getUsername());
            String token = jwtUtils.generateToken(user);

            return AuthResponseDto.builder()
                    .token(token)
                    .username(user.getUsername())
                    .build();
        } catch (AuthenticationException e) {
            log.warn("Error de autenticación para usuario {}: {}", request.getUsername(), e.getMessage());
            throw new BadRequestException("Credenciales inválidas");
        }
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> {
                    log.warn("Usuario no encontrado: {}", username);
                    return new UsernameNotFoundException("Usuario no encontrado: " + username);
                });
    }
}