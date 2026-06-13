package com.cordillera.bff.controller;

import com.cordillera.bff.dto.LoginRequestDTO;
import com.cordillera.bff.dto.SignupRequestDTO;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthBFFController {

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper(); // Para mapear JSON de forma segura

    // Dirección exacta de tu microservicio de autenticación
    private final String AUTH_SERVICE_URL = "http://localhost:8091/api/auth";

    @PostMapping("/register")
    public ResponseEntity<?> registrarUsuario(

            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @RequestBody SignupRequestDTO signupRequest) {
        System.out.println("========== REGISTER ==========");
        System.out.println("AUTH HEADER: " + authHeader);
        System.out.println("USERNAME: " + signupRequest.getUsername());
        System.out.println("EMAIL: " + signupRequest.getEmail());
        System.out.println("ROLES: " + signupRequest.getRoles());
        System.out.println("SUCURSAL: " + signupRequest.getSucursalId());
        System.out.println("==============================");

        try {

            System.out.println("TOKEN RECIBIDO EN BFF: " + authHeader);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            // Reenviar JWT al microservicio
            if (authHeader != null) {
                headers.set("Authorization", authHeader);
            }

            HttpEntity<SignupRequestDTO> request =
                    new HttpEntity<>(signupRequest, headers);

            ResponseEntity<String> response = restTemplate.postForEntity(
                    AUTH_SERVICE_URL + "/register",
                    request,
                    String.class
            );

            return ResponseEntity.ok(
                    Map.of("mensaje", "Usuario creado exitosamente")
            );

        } catch (HttpClientErrorException e) {

            System.out.println("ERROR AUTH MS:");
            System.out.println(e.getResponseBodyAsString());

            return ResponseEntity.status(e.getStatusCode()).body(
                    Map.of("error", e.getResponseBodyAsString())
            );

        } catch (Exception e) {

            e.printStackTrace();

            return ResponseEntity.status(500).body(
                    Map.of("error",
                            "Error interno al crear usuario: " + e.getMessage())
            );
        }
    }

    @PostMapping("/login")
    public ResponseEntity<?> loginFromFrontend(@RequestBody LoginRequestDTO loginRequest, HttpServletResponse response) {
        try {
            // 1. Enviamos el DTO al microservicio
            HttpEntity<LoginRequestDTO> request = new HttpEntity<>(loginRequest);

            // Recibimos la respuesta como String nativo para evitar colisiones de clases DTO distintas entre proyectos
            ResponseEntity<String> authResponse =
                    restTemplate.postForEntity(
                            AUTH_SERVICE_URL + "/login",
                            request,
                            String.class
                    );

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
    @GetMapping("/users")
    public String test() {

        System.out.println("================================");
        System.out.println("ENTRO AL ENDPOINT USERS");
        System.out.println("================================");

        return "OK";
    }
}