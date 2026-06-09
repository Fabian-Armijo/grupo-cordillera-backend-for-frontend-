package filter;

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

    // IMPORTANTÍSIMO: Sigue siendo tu misma clave secreta
    private final String SECRET_KEY = "MiClaveSecretaSuperSeguraQueNadiePuedeAdivinarYEsMuyLarga123456";

    @Override
    public ServerResponse filter(ServerRequest request, HandlerFunction<ServerResponse> next) throws Exception {

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
            // 3. Validar el JWT usando la firma secreta (Tu misma lógica exacta)
            SecretKey key = Keys.hmacShaKeyFor(SECRET_KEY.getBytes(StandardCharsets.UTF_8));

            Jwts.parserBuilder()
                    .setSigningKey(key)
                    .build()
                    .parseClaimsJws(token);

        } catch (Exception e) {
            // Si el token expiró, fue alterado o es falso, rebota la petición de inmediato
            return ServerResponse.status(HttpStatus.UNAUTHORIZED)
                    .body("Token no válido o expirado");
        }

        // Si todo está bien, dejamos que la petición continúe hacia el microservicio final
        return next.handle(request);
    }
}