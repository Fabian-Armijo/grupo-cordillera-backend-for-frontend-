package filter;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.cloud.gateway.server.mvc.common.MvcUtils;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.function.HandlerFilterFunction;
import org.springframework.web.servlet.function.HandlerFunction;
import org.springframework.web.servlet.function.ServerRequest;
import org.springframework.web.servlet.function.ServerResponse;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

@Component
public class AuthenticationFilter implements HandlerFilterFunction<ServerResponse, ServerResponse> {

    private final String SECRET_KEY = "MiClaveSecretaSuperSeguraQueNadiePuedeAdivinarYEsMuyLarga123456";

    @Override
    public ServerResponse filter(ServerRequest request, HandlerFunction<ServerResponse> next) throws Exception {

        // 🌟 EXCEPCIÓN: Si la ruta es el acumulador de KPIs, la dejamos pasar sin validar Token
        String path = request.path();
        if ("/api/kpi/acumular".equals(path)) {
            return next.handle(request);
        }

        // 1. Verificar si la cabecera Authorization viene en la petición
        Optional<String> authHeaderOpt = Optional.ofNullable(request.headers().firstHeader(HttpHeaders.AUTHORIZATION));

        if (authHeaderOpt.isEmpty()) {
            return ServerResponse.status(HttpStatus.UNAUTHORIZED)
                    .body("Falta la cabecera de autorización");
        }

        String authHeader = authHeaderOpt.get();

        // 2. Verificar que empiece con "Bearer "
        if (!authHeader.startsWith("Bearer ")) {
            return ServerResponse.status(HttpStatus.UNAUTHORIZED)
                    .body("Formato de token inválido");
        }

        String token = authHeader.substring(7);

        try {
            // 3. Validar el JWT usando la firma secreta
            SecretKey key = Keys.hmacShaKeyFor(SECRET_KEY.getBytes(StandardCharsets.UTF_8));

            // 🌟 CORRECCIÓN CRÍTICA: Extraemos los Claims (datos dentro del JWT) al validar
            Claims claims = Jwts.parserBuilder()
                    .setSigningKey(key)
                    .build()
                    .parseClaimsJws(token)
                    .getBody();

            // 🌟 CORRECCIÓN CRÍTICA: Obtenemos el Rol y el Sucursal ID mapeados en tu token
            // Nota: Ajusta los nombres "role" y "sucursalId" si en tu MS-AUTH los firmaste con otro nombre (ej: "rol", "sucursal")
            String userRole = claims.get("role", String.class);
            Object sucursalIdObj = claims.get("sucursalId");
            String sucursalId = sucursalIdObj != null ? String.valueOf(sucursalIdObj) : null;

            // 🌟 CORRECCIÓN CRÍTICA: Mutamos la petición inyectando los headers que espera MS-VENTAS
            ServerRequest requestMutada = ServerRequest.from(request)
                    .header("X-User-Role", userRole != null ? userRole : "")
                    .header("X-Sucursal-Id", sucursalId != null ? sucursalId : "")
                    .build();

            // Devolvemos el flujo usando la petición inyectada con las nuevas cabeceras
            return next.handle(requestMutada);

        } catch (Exception e) {
            System.err.println("🚨 Error al procesar o inyectar claims del JWT: " + e.getMessage());
            return ServerResponse.status(HttpStatus.UNAUTHORIZED)
                    .body("Token no válido o expirado");
        }
    }
}