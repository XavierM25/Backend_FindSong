package com.findsong.findsongapi.auth.service;

import java.util.List;
import com.findsong.findsongapi.auth.dto.AuthResponseDto;
import com.findsong.findsongapi.auth.dto.LoginRequestDto;
import com.findsong.findsongapi.auth.dto.RegisterRequestDto;
import com.findsong.findsongapi.auth.model.User;
import com.findsong.findsongapi.auth.repository.UserRepository;
import com.findsong.findsongapi.auth.security.JwtUtils;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
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
        User user = User.builder()
                .username(request.getUsername())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .roles(List.of("ROLE_USER"))
                .build();
        userRepository.save(user);
        String token = jwtUtils.generateToken(user);
        return AuthResponseDto.builder()
                .token(token)
                .username(user.getUsername())
                .build();
    }

    public AuthResponseDto login(LoginRequestDto request) {
        if (authenticationManager == null) {
            throw new IllegalStateException("AuthenticationManager no ha sido inicializado");
        }

        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword())
        );

        User user = userRepository.findByUsername(request.getUsername())
                .orElseThrow(() -> new UsernameNotFoundException("Usuario no encontrado"));

        String token = jwtUtils.generateToken(user);
        return AuthResponseDto.builder()
                .token(token)
                .username(user.getUsername())
                .build();
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("Usuario no encontrado"));
    }
}