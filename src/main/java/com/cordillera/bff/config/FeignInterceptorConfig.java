package com.cordillera.bff.config;

import feign.RequestInterceptor;
import feign.RequestTemplate;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Configuration
public class FeignInterceptorConfig implements RequestInterceptor {

    @Override
    public void apply(RequestTemplate template) {
        // 1. Obtenemos el HttpServletRequest de forma segura para el hilo actual (ThreadLocal)
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();

        if (attributes != null) {
            HttpServletRequest request = attributes.getRequest();

            // 2. Extraemos el JWT del hilo de la petición que guardó el TokenRelayInterceptor
            String jwtToken = (String) request.getAttribute("INTERNAL_JWT");

            if (jwtToken != null) {
                // 🌟 Removemos cualquier intento de Authorization previo (como el Basic Auth)
                // para que no choquen en el microservicio destino
                template.removeHeader("Authorization");

                // 3. Inyectamos el Bearer Token definitivo
                template.header("Authorization", "Bearer " + jwtToken);
            }
        }
    }
}