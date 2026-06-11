package com.cordillera.bff.controller;

import com.cordillera.bff.dto.LoginRequestDTO;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

@RestController
@RequestMapping("/api/auth")
public class AuthBFFController {

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper(); // Para mapear JSON de forma segura

    // Dirección exacta de tu microservicio de autenticación
    private final String AUTH_SERVICE_URL = "http://localhost:8091/api/auth/login";

    @PostMapping("/login")
    public ResponseEntity<?> loginFromFrontend(@RequestBody LoginRequestDTO loginRequest, HttpServletResponse response) {
        try {
            // 1. Enviamos el DTO al microservicio
            HttpEntity<LoginRequestDTO> request = new HttpEntity<>(loginRequest);

            // Recibimos la respuesta como String nativo para evitar colisiones de clases DTO distintas entre proyectos
            ResponseEntity<String> authResponse = restTemplate.postForEntity(AUTH_SERVICE_URL, request, String.class);

            // 2. Parseamos el JSON de manera dinámica usando Jackson
            JsonNode rootNode = objectMapper.readTree(authResponse.getBody());

            // Buscamos el token. Si en tu microservicio se llama "token" o "accessToken", búscalo correspondientemente aquí:
            String jwtToken = rootNode.has("token") ? rootNode.get("token").asText() : rootNode.get("accessToken").asText();

            // 3. Inyectamos la Cookie HttpOnly blindada para el Frontend
            Cookie cookie = new Cookie("BFF_SESSION", jwtToken);
            cookie.setHttpOnly(true);
            cookie.setSecure(false); // Pon 'true' si usas HTTPS en el futuro
            cookie.setPath("/");
            cookie.setMaxAge(24 * 60 * 60);

            response.addCookie(cookie);

            // 4. Le devolvemos el cuerpo completo tal cual al Frontend en React
            return ResponseEntity.ok(authResponse.getBody());

        } catch (org.springframework.web.client.HttpClientErrorException e) {
            // 🚨 Si el microservicio responde 401 o 403, imprímelo explícitamente en la consola de IntelliJ
            System.err.println("❌ El Microservicio de Autenticación denegó el acceso. Código: " + e.getStatusCode());
            System.err.println("Respuesta del Microservicio: " + e.getResponseBodyAsString());
            return ResponseEntity.status(e.getStatusCode()).body(e.getResponseBodyAsString());

        } catch (Exception e) {
            // 🚨 Captura cualquier otro error mecánico (Base de datos caída, error de mapeo, etc.)
            System.err.println("❌ ERROR INTERNO EN EL PROCESO DEL BFF:");
            e.printStackTrace();
            return ResponseEntity.status(500).body("Error interno en el BFF: " + e.getMessage());
        }
    }
}