package com.cordillera.bff;
import com.cordillera.bff.controller.AuthBFFController;

import com.cordillera.bff.dto.JwtResponseDTO;
import com.cordillera.bff.dto.LoginRequestDTO;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.client.RestTemplate;

import java.lang.reflect.Field;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withUnauthorizedRequest;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

@SpringBootTest
@AutoConfigureMockMvc
public class AuthBFFControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private AuthBFFController authBFFController;

    private MockRestServiceServer mockServer;
    private ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    public void setUp() throws Exception {
        // Usamos reflexión para obtener el RestTemplate interno del controlador y simular el servidor externo
        Field field = AuthBFFController.class.getDeclaredField("restTemplate");
        field.setAccessible(true);
        RestTemplate restTemplate = (RestTemplate) field.get(authBFFController);
        mockServer = MockRestServiceServer.createServer(restTemplate);
    }

    @Test
    public void testLogin_Success() throws Exception {
        // 1. Preparar datos de entrada del Frontend
        LoginRequestDTO loginRequest = new LoginRequestDTO();
        loginRequest.setUsername("joaquin");
        loginRequest.setPassword("password123");

        // 2. Preparar respuesta simulada que devolvería el Microservicio de Autenticación
        java.util.List<String> mockRoles = java.util.Arrays.asList("ROLE_USER");
        JwtResponseDTO mockResponse = new JwtResponseDTO(
                "mocked-jwt-token-xyz-12345", // accessToken (el JWT)
                1L,                            // id
                "joaquin",                     // username
                "joaquin@correo.com",          // email
                mockRoles                      // roles
        );

        // 3. Configurar el servidor falso para interceptar la llamada al puerto 8088
        mockServer.expect(requestTo("http://localhost:8088/api/auth/login"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess(objectMapper.writeValueAsString(mockResponse), MediaType.APPLICATION_JSON));

        // 4. Ejecutar la petición simulada al BFF y validar el resultado
        mockMvc.perform(post("/bff/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("mocked-jwt-token-xyz-12345"))
                .andDo(result -> {
                    // Validar que la Cookie HttpOnly BFF_SESSION se haya creado correctamente
                    Cookie cookie = result.getResponse().getCookie("BFF_SESSION");
                    assertNotNull(cookie);
                    assertEquals("mocked-jwt-token-xyz-12345", cookie.getValue());
                    assertEquals(true, cookie.isHttpOnly());
                });

        mockServer.verify();
    }

    @Test
    public void testLogin_InvalidCredentials() throws Exception {
        // 1. Preparar datos con credenciales erróneas
        LoginRequestDTO loginRequest = new LoginRequestDTO();
        loginRequest.setUsername("intruso");
        loginRequest.setPassword("incorrecta");

        // 2. Configurar el servidor falso para que devuelva un error 401
        mockServer.expect(requestTo("http://localhost:8088/api/auth/login"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withUnauthorizedRequest());

        // 3. Ejecutar y validar que el BFF capture la excepción y responda 401
        mockMvc.perform(post("/bff/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isUnauthorized());

        mockServer.verify();
    }
}