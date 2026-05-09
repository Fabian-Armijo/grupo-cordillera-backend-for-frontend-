package com.cordillera.bff.config;


import feign.auth.BasicAuthRequestInterceptor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class FeignClientConfig {

    @Value("${microservices.security.user}")
    private String user;

    @Value("${microservices.security.password}")
    private String password;

    @Bean
    public BasicAuthRequestInterceptor basicAuthRequestInterceptor() {
        // Ahora el BFF usará la contraseña que CADA UNO tenga en su PC
        return new BasicAuthRequestInterceptor(user, password);
    }
}