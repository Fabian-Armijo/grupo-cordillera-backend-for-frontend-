package com.cordillera.bff.config;

import com.cordillera.bff.service.JwtService;
import feign.RequestInterceptor;
import feign.RequestTemplate;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component
public class FeignInterceptorConfig implements RequestInterceptor {

    @Autowired
    private JwtService jwtService;

    @Override
    public void apply(RequestTemplate template) {
        String jwtToken = null;
        String sucursalHeaderManual = null;

        // 🔒 PRIORIDAD 1: Buscar en el contexto de Spring Security (Heredado en hilos hijos)
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getCredentials() != null) {
            jwtToken = auth.getCredentials().toString();
        }

        // 🛠️ PRIORIDAD 2: Si falló, buscamos en el Request HTTP original
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes != null) {
            HttpServletRequest request = attributes.getRequest();

            // Guardamos la sucursal manual enviada por React por si el token falla
            sucursalHeaderManual = request.getHeader("X-Sucursal-Id");

            if (jwtToken == null || jwtToken.trim().isEmpty() || "anonymousUser".equals(jwtToken)) {
                String authHeader = request.getHeader("Authorization");
                if (authHeader != null && authHeader.startsWith("Bearer ")) {
                    jwtToken = authHeader.substring(7);
                } else if (request.getAttribute("INTERNAL_JWT") != null) {
                    jwtToken = (String) request.getAttribute("INTERNAL_JWT");
                }
            }
        }

        // ✈️ ENVIAR DATOS REALES SI SE ENCONTRÓ EL TOKEN
        if (jwtToken != null && !jwtToken.trim().isEmpty() && !"anonymousUser".equals(jwtToken)) {
            try {
                String rol = jwtService.extractRole(jwtToken);
                Long sucursalId = jwtService.extractSucursalId(jwtToken);

                // Si por alguna razón el JWT no tiene sucursal pero React la mandó, la usamos de salvavidas
                if (sucursalId == null && sucursalHeaderManual != null) {
                    sucursalId = Long.valueOf(sucursalHeaderManual);
                }

                System.out.println("[🚀 INTERCEPTOR COMPARTIDO] -> Inyectando Rol Real: " + rol + " | Sucursal Real: " + sucursalId);

                template.header("X-User-Role", rol);
                if (sucursalId != null) {
                    template.header("X-Sucursal-Id", String.valueOf(sucursalId));
                }
                template.header("Authorization", "Bearer " + jwtToken);
                return;
            } catch (Exception e) {
                System.out.println("[⚠️ INTERCEPTOR] Error al procesar token heredado: " + e.getMessage());
            }
        }

        // 🚨 RESPALDO CRÍTICO: Si el token falló por completo debido a los hilos de Feign,
        // propagamos la información utilizando la autenticación segura del contexto actual o el header directo.
        if (auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getName())) {
            String rolCompleto = auth.getAuthorities().iterator().next().getAuthority();
            String rolLimpio = rolCompleto.replace("ROLE_", "");

            template.header("X-User-Role", rolLimpio);
            if (sucursalHeaderManual != null) {
                template.header("X-Sucursal-Id", sucursalHeaderManual);
            }
            System.out.println("[🛡️ FALLBACK INTERCEPTOR] -> Propagando mediante contexto Spring: Rol: " + rolLimpio + " | Sucursal: " + sucursalHeaderManual);
        } else {
            System.out.println("[❌ INTERCEPTOR CRÍTICO] No se pudo rescatar la sesión en este hilo.");
        }
    }
}