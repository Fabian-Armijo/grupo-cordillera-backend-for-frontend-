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
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import java.io.IOException;
import java.util.Collections;
import java.util.List;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(JwtAuthenticationFilter.class);
    private final JwtService jwtService;

    public JwtAuthenticationFilter(JwtService jwtService) {
        this.jwtService = jwtService;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) throws ServletException {
        String path = request.getRequestURI();
        return request.getRequestURI().equals("/api/auth/login");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        String authHeader = request.getHeader("Authorization");
        System.out.println("AUTH HEADER RECIBIDO: " + request.getHeader("Authorization"));
        String path = request.getRequestURI();

        log.info("🔍 [BFF-JWT] Evaluando petición en la ruta: {}", path);

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            log.warn("⚠️ [BFF-JWT] No se detectó cabecera Authorization o no es tipo 'Bearer ' en ruta: {}", path);
            filterChain.doFilter(request, response);
            return;
        }

        try {
            String jwt = authHeader.substring(7);
            String username = jwtService.extractUsername(jwt);
            log.info("👤 [BFF-JWT] Usuario extraído del token: {}", username);

            if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {

                boolean isValid = jwtService.isTokenValid(jwt);
                log.info("🔑 [BFF-JWT] ¿El token es válido para este BFF?: {}", isValid);

                if (isValid) {
                    String rolDesdeToken = jwtService.extractRole(jwt);
                    log.info("🏷️ [BFF-JWT] Rol crudo leído del JWT: '{}'", rolDesdeToken);

                    if (rolDesdeToken == null || rolDesdeToken.trim().isEmpty()) {
                        log.error("❌ [BFF-JWT] El rol se leyó como NULL o VACÍO. Spring Security negará el acceso.");
                        filterChain.doFilter(request, response);
                        return;
                    }

                    // 🔄 Sanitización estricta del prefijo
                    String rolFormateado = rolDesdeToken.trim();
                    if (!rolFormateado.toUpperCase().startsWith("ROLE_")) {
                        rolFormateado = "ROLE_" + rolFormateado;
                    }
                    rolFormateado = rolFormateado.toUpperCase();

                    log.info("🛡️ [BFF-JWT] Inyectando Autoridad Limpia en Spring Security: '{}'", rolFormateado);

                    List<SimpleGrantedAuthority> authorities = Collections.singletonList(
                            new SimpleGrantedAuthority(rolFormateado)
                    );

                    UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                            username,
                            jwt,
                            authorities
                    );

                    authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    SecurityContextHolder.getContext().setAuthentication(authToken);
                    log.info("✅ [BFF-JWT] Contexto de seguridad establecido con éxito para: {}", username);
                } else {
                    log.error("❌ [BFF-JWT] El Token no es válido (Firma errónea o expirado). Revisa las llaves secretas.");
                }
            }
        } catch (Exception e) {
            log.error("💥 [BFF-JWT] Excepción crítica procesando la verificación del JWT: ", e);
        }

        filterChain.doFilter(request, response);
    }
}