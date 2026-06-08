package com.cordillera.bff.controller;

import com.cordillera.bff.dto.JwtResponseDTO;
import com.cordillera.bff.dto.LoginRequestDTO;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

@RestController
@RequestMapping("/bff")
public class AuthBFFController {

    private final RestTemplate restTemplate = new RestTemplate();

    // URL real de tu microservicio de autenticación (pasando por el Gateway o directo)
    private final String AUTH_SERVICE_URL = "http://localhost:8080/api/auth/login";

    @PostMapping("/login")
    public ResponseEntity<?> loginFromFrontend(@RequestBody LoginRequestDTO loginRequest, HttpServletResponse response) {

        try {
            // 1. Enviamos los datos al microservicio de autenticación original
            HttpEntity<LoginRequestDTO> request = new HttpEntity<>(loginRequest);
            ResponseEntity<JwtResponseDTO> authResponse = restTemplate.postForEntity(AUTH_SERVICE_URL, request, JwtResponseDTO.class);

            // 2. Extraemos el JWT que nos devolvió tu microservicio
            String jwtToken = authResponse.getBody().getToken();

            // 3. ¡MÁGIA DE SEGURIDAD! Metemos el JWT dentro de una Cookie HttpOnly
            Cookie cookie = new Cookie("BFF_SESSION", jwtToken);
            cookie.setHttpOnly(true);       // Impide que JavaScript (Frontend) robe el token
            cookie.setSecure(false);        // Cambiar a 'true' en producción cuando uses HTTPS
            cookie.setPath("/");            // Disponible para todo el dominio
            cookie.setMaxAge(24 * 60 * 60); // Duración de 24 horas (igual que tu token)

            // 4. Añadimos la cookie a la respuesta que va hacia el Frontend
            response.addCookie(cookie);

            // Al frontend solo le devolvemos los datos públicos del usuario, ¡NUNCA el token!
            return ResponseEntity.ok(authResponse.getBody());

        } catch (Exception e) {
            return ResponseEntity.status(401).body("Credenciales inválidas en el BFF");
        }
    }
}