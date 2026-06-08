package com.cordillera.bff.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    @Autowired
    private TokenRelayInterceptor tokenRelayInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // Le decimos a Spring que aplique el interceptor a todas las rutas excepto al Login
        registry.addInterceptor(tokenRelayInterceptor)
                .addPathPatterns("/**")
                .excludePathPatterns("/bff/login"); // Ajusta esta ruta si tu login se llama diferente
    }
}