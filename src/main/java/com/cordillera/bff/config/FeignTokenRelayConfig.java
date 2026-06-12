package com.cordillera.bff.config;

import feign.RequestInterceptor;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Configuration
public class FeignTokenRelayConfig {

    @Bean
    public RequestInterceptor feignRequestInterceptor() {
        return requestTemplate -> {
            // 1. Capturamos la petición HTTP actual que viene desde el Frontend (React)
            ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();

            if (attributes != null) {
                HttpServletRequest request = attributes.getRequest();

                // 2. Rescatamos el JWT que tu TokenRelayInterceptor guardó con éxito en el preHandle
                String jwtToken = (String) request.getAttribute("INTERNAL_JWT");

                // 3. Si existe, se lo inyectamos a Feign para que viaje hacia ms-productos y ms-stock
                if (jwtToken != null) {
                    requestTemplate.header("Authorization", "Bearer " + jwtToken);
                    System.out.println("🚀 [FEIGN-GATEWAY] -> Token retransmitido con éxito hacia el microservicio.");
                } else {
                    // Contingencia por si no se procesó el interceptor, intentamos leer directo del header original
                    String authHeader = request.getHeader("Authorization");
                    if (authHeader != null) {
                        requestTemplate.header("Authorization", authHeader);
                        System.out.println("🚀 [FEIGN-GATEWAY] -> Token heredado directamente del header original.");
                    }
                }
            }
        };
    }
}
