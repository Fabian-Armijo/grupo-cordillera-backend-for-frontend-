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
        // 1. Extraemos el JWT que nuestro TokenRelayInterceptor guardó en el hilo de la petición
        String jwtToken = (String) request.getAttribute("INTERNAL_JWT");

        // 2. Si el token existe, se lo inyectamos automáticamente a la llamada de Feign
        if (jwtToken != null) {
            template.header("Authorization", "Bearer " + jwtToken);
        }
    }
}