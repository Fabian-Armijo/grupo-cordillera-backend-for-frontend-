package com.cordillera.bff.config;

import feign.RequestInterceptor;
import feign.RequestTemplate;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;

@Configuration
public class FeignInterceptorConfig implements RequestInterceptor {

    @Autowired
    private HttpServletRequest request;

    @Override
    public void apply(RequestTemplate template) {
        // 1. Extraemos el JWT del hilo de la petición
        String jwtToken = (String) request.getAttribute("INTERNAL_JWT");

        if (jwtToken != null) {
            // 🌟 Removemos cualquier intento de Authorization previo (como el Basic Auth)
            // para que no choquen en el microservicio destino
            template.removeHeader("Authorization");

            // 2. Inyectamos el Bearer Token definitivo
            template.header("Authorization", "Bearer " + jwtToken);
        }
    }
}