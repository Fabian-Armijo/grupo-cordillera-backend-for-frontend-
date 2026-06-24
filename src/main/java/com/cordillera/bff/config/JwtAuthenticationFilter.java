package com.cordillera.bff.config;

import com.cordillera.bff.service.JwtService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(JwtAuthenticationFilter.class);

    private final JwtService jwtService;

    public JwtAuthenticationFilter(JwtService jwtService) {
        this.jwtService = jwtService;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return request.getRequestURI().equals("/api/auth/login");
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {

        String authHeader = request.getHeader("Authorization");

        System.out.println("AUTH HEADER RECIBIDO: " + authHeader);

        String path = request.getRequestURI();

        log.info("🔍 [BFF-JWT] Evaluando petición en la ruta: {}", path);

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {

            log.warn(
                    "⚠️ [BFF-JWT] No se detectó cabecera Authorization o no es tipo 'Bearer ' en ruta: {}",
                    path
            );

            filterChain.doFilter(request, response);
            return;
        }

        try {

            String jwt = authHeader.substring(7);

            String username = jwtService.extractUsername(jwt);

            log.info("👤 [BFF-JWT] Usuario extraído del token: {}", username);

            if (username != null
                    && SecurityContextHolder.getContext().getAuthentication() == null) {

                boolean isValid = jwtService.isTokenValid(jwt);

                log.info(
                        "🔑 [BFF-JWT] ¿El token es válido para este BFF?: {}",
                        isValid
                );

                if (isValid) {

                    String rolDesdeToken = jwtService.extractRole(jwt);

                    Long sucursalId = jwtService.extractSucursalId(jwt);

                    log.info(
                            "🏷️ [BFF-JWT] Rol leído desde JWT: '{}'",
                            rolDesdeToken
                    );

                    log.info(
                            "🏢 [BFF-JWT] Sucursal leída desde JWT: '{}'",
                            sucursalId
                    );

                    System.out.println("======================================");
                    System.out.println("USUARIO JWT: " + username);
                    System.out.println("ROL JWT: " + rolDesdeToken);
                    System.out.println("SUCURSAL JWT: " + sucursalId);
                    System.out.println("======================================");

                    if (rolDesdeToken == null
                            || rolDesdeToken.trim().isEmpty()) {

                        log.error(
                                "❌ [BFF-JWT] El rol se leyó como NULL o VACÍO."
                        );

                        filterChain.doFilter(request, response);
                        return;
                    }

                    String rolFormateado = rolDesdeToken.trim();

                    if (!rolFormateado.toUpperCase().startsWith("ROLE_")) {
                        rolFormateado = "ROLE_" + rolFormateado;
                    }

                    rolFormateado = rolFormateado.toUpperCase();

                    List<SimpleGrantedAuthority> authorities =
                            Collections.singletonList(
                                    new SimpleGrantedAuthority(rolFormateado)
                            );

                    UsernamePasswordAuthenticationToken authToken =
                            new UsernamePasswordAuthenticationToken(
                                    username,
                                    jwt,
                                    authorities
                            );

                    // 🔥 Guardamos datos extra en el SecurityContext
                    Map<String, Object> details = new HashMap<>();

                    details.put("sucursalId", sucursalId);
                    details.put("username", username);
                    details.put("role", rolDesdeToken);

                    authToken.setDetails(details);

                    SecurityContextHolder.getContext()
                            .setAuthentication(authToken);

                    log.info(
                            "✅ [BFF-JWT] Contexto de seguridad establecido con éxito para: {}",
                            username
                    );

                } else {

                    log.error(
                            "❌ [BFF-JWT] El token no es válido o expiró."
                    );
                }
            }

        } catch (Exception e) {

            log.error(
                    "💥 [BFF-JWT] Error procesando JWT",
                    e
            );
        }

        filterChain.doFilter(request, response);
    }
}