package com.findsong.findsongapi.auth.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.stereotype.Component;

@Component
public class AuthServiceInitializer implements ApplicationListener<ContextRefreshedEvent> {

    private final AuthService authService;
    private final AuthenticationManager authenticationManager;

    @Autowired
    public AuthServiceInitializer(AuthService authService, AuthenticationManager authenticationManager) {
        this.authService = authService;
        this.authenticationManager = authenticationManager;
    }

    @Override
    public void onApplicationEvent(ContextRefreshedEvent event) {
        authService.setAuthenticationManager(authenticationManager);
    }
}