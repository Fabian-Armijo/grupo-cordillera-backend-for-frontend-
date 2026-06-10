package com.cordillera.bff.config;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class TokenRelayInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String jwtToken = null;

        // 🌟 A. Intentar leer desde el Header Authorization (Lo que manda tu Frontend actual)
        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            jwtToken = authHeader.substring(7); // Cortamos la palabra "Bearer "
        }

        // B. Si no venía en el header, buscamos en las cookies como respaldo
        if (jwtToken == null) {
            Cookie[] cookies = request.getCookies();
            if (cookies != null) {
                for (Cookie cookie : cookies) {
                    if ("BFF_SESSION".equals(cookie.getName())) {
                        jwtToken = cookie.getValue();
                        break;
                    }
                }
            }
        }

        // 2. Si logramos rescatar el JWT, lo guardamos en el hilo de la petición
        if (jwtToken != null) {
            request.setAttribute("INTERNAL_JWT", jwtToken);
        }

        return true;
    }
}