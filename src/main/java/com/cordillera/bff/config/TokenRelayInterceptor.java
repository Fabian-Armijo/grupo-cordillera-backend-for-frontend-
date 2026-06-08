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
        // 1. interceptamos la petición del Frontend y buscamos la cookie segura
        Cookie[] cookies = request.getCookies();
        String jwtToken = null;

        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if ("BFF_SESSION".equals(cookie.getName())) {
                    jwtToken = cookie.getValue();
                    break;
                }
            }
        }

        // 2. Si la cookie existe, guardamos el JWT en el "hilo de la petición" (Request Attribute)
        // para que tus servicios (como Catálogo o Ventas) puedan sacarlo fácilmente
        if (jwtToken != null) {
            request.setAttribute("INTERNAL_JWT", jwtToken);
        }

        return true; // Permitir que la petición siga hacia tu controlador
    }
}