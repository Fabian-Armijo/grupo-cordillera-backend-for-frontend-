package com.cordillera.bff;

import com.cordillera.bff.config.TokenRelayInterceptor;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class TokenRelayInterceptorTest {

    @Test
    public void testPreHandle_WithBffSessionCookie_ShouldSetRequestAttribute() throws Exception {
        // 1. Simular los objetos HTTP usando Mockito
        HttpServletRequest mockRequest = Mockito.mock(HttpServletRequest.class);
        HttpServletResponse mockResponse = Mockito.mock(HttpServletResponse.class);
        Object mockHandler = Mockito.mock(Object.class);

        // 2. Crear la cookie falsa con un token de prueba
        Cookie bffCookie = new Cookie("BFF_SESSION", "mocked-jwt-token-12345");
        Cookie[] cookies = new Cookie[]{bffCookie};

        when(mockRequest.getCookies()).thenReturn(cookies);

        // 3. Instanciar nuestro interceptor real
        TokenRelayInterceptor interceptor = new TokenRelayInterceptor();

        // 4. Ejecutar el método preHandle
        boolean result = interceptor.preHandle(mockRequest, mockResponse, mockHandler);

        // 5. Verificar que el método devolvió true (deja pasar la petición)
        assertTrue(result);

        // 6. Verificar que se guardó el token en los atributos de la petición con la clave correcta
        verify(mockRequest).setAttribute("INTERNAL_JWT", "mocked-jwt-token-12345");
    }

    @Test
    public void testPreHandle_WithoutCookies_ShouldNotSetAttribute() throws Exception {
        HttpServletRequest mockRequest = Mockito.mock(HttpServletRequest.class);
        HttpServletResponse mockResponse = Mockito.mock(HttpServletResponse.class);
        Object mockHandler = Mockito.mock(Object.class);

        // Simular que no vienen cookies en la petición
        when(mockRequest.getCookies()).thenReturn(null);

        TokenRelayInterceptor interceptor = new TokenRelayInterceptor();

        boolean result = interceptor.preHandle(mockRequest, mockResponse, mockHandler);

        assertTrue(result);
        // Verificar que NUNCA se llamó al setAttribute si no había cookies
        Mockito.verify(mockRequest, Mockito.never()).setAttribute(Mockito.anyString(), Mockito.anyString());
    }
}