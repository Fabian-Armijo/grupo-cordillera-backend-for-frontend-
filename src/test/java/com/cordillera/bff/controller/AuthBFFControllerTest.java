package com.cordillera.bff.controller;

import com.cordillera.bff.config.JwtAuthenticationFilter;
import com.cordillera.bff.config.SecurityConfig;
import com.cordillera.bff.config.TokenRelayInterceptor;
import com.cordillera.bff.dto.JwtResponseDTO;
import com.cordillera.bff.dto.LoginRequestDTO;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.SecurityFilterAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// 🛡️ Aislamiento total: Apagamos la seguridad y excluimos el SecurityConfig para que no pida dependencias
@WebMvcTest(
        controllers = AuthBFFController.class,
        excludeAutoConfiguration = {SecurityAutoConfiguration.class, SecurityFilterAutoConfiguration.class},
        excludeFilters = @ComponentScan.Filter(
                type = FilterType.ASSIGNABLE_TYPE,
                classes = {SecurityConfig.class, JwtAuthenticationFilter.class, TokenRelayInterceptor.class}
        )
)
public class AuthBFFControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private AuthBFFController authBFFController;

    private RestTemplate mockRestTemplate;
    private ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    public void setUp() {
        // Creamos el mock y lo inyectamos directamente con ReflectionTestUtils
        mockRestTemplate = Mockito.mock(RestTemplate.class);
        ReflectionTestUtils.setField(authBFFController, "restTemplate", mockRestTemplate);
    }

    @Test
    public void testLogin_Success() throws Exception {
        LoginRequestDTO loginRequest = new LoginRequestDTO();
        loginRequest.setUsername("joaquin");
        loginRequest.setPassword("password123");

        List<String> mockRoles = Arrays.asList("ROLE_USER");
        JwtResponseDTO mockResponse = new JwtResponseDTO(
                "mocked-jwt-token-xyz-12345", 1L, "joaquin", "joaquin@correo.com", mockRoles
        );

        ResponseEntity<String> simulatedResponse = ResponseEntity.ok(objectMapper.writeValueAsString(mockResponse));

        when(mockRestTemplate.postForEntity(
                eq("http://localhost:8091/api/auth/login"),
                any(HttpEntity.class),
                eq(String.class)
        )).thenReturn(simulatedResponse);

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("mocked-jwt-token-xyz-12345")) // 👈 CAMBIA ESTO AQUÍ
                .andDo(result -> {
                    Cookie cookie = result.getResponse().getCookie("BFF_SESSION");
                    assertNotNull(cookie);
                    assertEquals("mocked-jwt-token-xyz-12345", cookie.getValue());
                    assertEquals(true, cookie.isHttpOnly());
                });
    }

    @Test
    public void testLogin_InvalidCredentials() throws Exception {
        LoginRequestDTO loginRequest = new LoginRequestDTO();
        loginRequest.setUsername("intruso");
        loginRequest.setPassword("incorrecta");

        HttpClientErrorException unauthorizedError = HttpClientErrorException.create(
                HttpStatus.UNAUTHORIZED, "Unauthorized", HttpHeaders.EMPTY, "Credenciales invalidas".getBytes(), Charset.defaultCharset()
        );

        when(mockRestTemplate.postForEntity(
                eq("http://localhost:8091/api/auth/login"),
                any(HttpEntity.class),
                eq(String.class)
        )).thenThrow(unauthorizedError);

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isUnauthorized());
    }
}