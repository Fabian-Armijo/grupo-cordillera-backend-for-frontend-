package com.cordillera.bff.config;

import jakarta.annotation.PostConstruct;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.core.context.SecurityContextHolder;

@Configuration
public class AsyncSecurityConfig {

    @PostConstruct
    public void enableAuthOnChildThreads() {
        // 🎯 ESTA ES LA MAGIA: Obliga a Spring Security a heredar todo el contexto de autenticación
        // (incluyendo el usuario y su token) a cualquier hilo secundario o asíncrono que se cree.
        SecurityContextHolder.setStrategyName(SecurityContextHolder.MODE_INHERITABLETHREADLOCAL);
    }
}
