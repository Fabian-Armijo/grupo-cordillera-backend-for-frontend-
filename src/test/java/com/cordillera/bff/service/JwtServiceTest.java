package com.cordillera.bff.service;

import com.cordillera.bff.service.JwtService;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Date;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("JwtService – Tests unitarios")
class JwtServiceTest {

    private JwtService jwtService;

    // La misma clave que usa el BFF por defecto en application.properties
    private static final String SECRET_KEY =
            "404E635266556A586E3272357538782F413F4428472B4B6250645367566B5970";

    @BeforeEach
    void setUp() {
        jwtService = new JwtService();
        ReflectionTestUtils.setField(jwtService, "secretKey", SECRET_KEY);
    }

    // ─── Helpers para construir tokens de prueba ─────────────────────────────

    private String buildToken(String subject, String rol, Long sucursalId, long expiresInMs) {
        return Jwts.builder()
                .setSubject(subject)
                .claim("roles", List.of(rol))
                .claim("sucursalId", sucursalId)
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + expiresInMs))
                .signWith(SignatureAlgorithm.HS256, SECRET_KEY.getBytes())
                .compact();
    }

    private String buildExpiredToken(String subject) {
        return Jwts.builder()
                .setSubject(subject)
                .claim("roles", List.of("ADMIN"))
                .setIssuedAt(new Date(System.currentTimeMillis() - 10_000))
                .setExpiration(new Date(System.currentTimeMillis() - 5_000))
                .signWith(SignatureAlgorithm.HS256, SECRET_KEY.getBytes())
                .compact();
    }

    // ─── extractUsername ─────────────────────────────────────────────────────

    @Nested
    @DisplayName("extractUsername")
    class ExtractUsername {

        @Test
        @DisplayName("Retorna el subject del token correctamente")
        void extractUsername_retornaSubject() {
            String token = buildToken("admin@cordillera.cl", "ADMIN", 1L, 60_000);
            assertThat(jwtService.extractUsername(token)).isEqualTo("admin@cordillera.cl");
        }

        @Test
        @DisplayName("Retorna el username para distintos usuarios")
        void extractUsername_distintosUsuarios() {
            String token = buildToken("cajero@sucursal2.cl", "USUARIO", 2L, 60_000);
            assertThat(jwtService.extractUsername(token)).isEqualTo("cajero@sucursal2.cl");
        }
    }

    // ─── extractRole ─────────────────────────────────────────────────────────

    @Nested
    @DisplayName("extractRole")
    class ExtractRole {

        @Test
        @DisplayName("Retorna 'ADMIN' cuando el token trae 'ROLE_ADMIN'")
        void extractRole_conPrefixRoleAdmin() {
            String token = buildToken("admin@cordillera.cl", "ROLE_ADMIN", 1L, 60_000);
            assertThat(jwtService.extractRole(token)).isEqualTo("ADMIN");
        }

        @Test
        @DisplayName("Retorna el rol sin cambios cuando no trae prefijo 'ROLE_'")
        void extractRole_sinPrefijo() {
            String token = buildToken("cajero@cordillera.cl", "USUARIO", 2L, 60_000);
            assertThat(jwtService.extractRole(token)).isEqualTo("USUARIO");
        }

        @Test
        @DisplayName("Retorna 'USER' por defecto cuando el token no trae claim 'roles'")
        void extractRole_sinClaimRoles_retornaUserPorDefecto() {
            String tokenSinRoles = Jwts.builder()
                    .setSubject("gerente@cordillera.cl")
                    .setExpiration(new Date(System.currentTimeMillis() + 60_000))
                    .signWith(SignatureAlgorithm.HS256, SECRET_KEY.getBytes())
                    .compact();

            assertThat(jwtService.extractRole(tokenSinRoles)).isEqualTo("USER");
        }
    }

    // ─── extractSucursalId ───────────────────────────────────────────────────

    @Nested
    @DisplayName("extractSucursalId")
    class ExtractSucursalId {

        @Test
        @DisplayName("Extrae correctamente el sucursalId del token")
        void extractSucursalId_retornaId() {
            String token = buildToken("cajero@cordillera.cl", "USUARIO", 3L, 60_000);
            assertThat(jwtService.extractSucursalId(token)).isEqualTo(3L);
        }

        @Test
        @DisplayName("Retorna null cuando el token no trae sucursalId")
        void extractSucursalId_sinClaim_retornaNull() {
            String tokenSinSucursal = Jwts.builder()
                    .setSubject("admin@cordillera.cl")
                    .claim("roles", List.of("ADMIN"))
                    .setExpiration(new Date(System.currentTimeMillis() + 60_000))
                    .signWith(SignatureAlgorithm.HS256, SECRET_KEY.getBytes())
                    .compact();

            assertThat(jwtService.extractSucursalId(tokenSinSucursal)).isNull();
        }
    }

    // ─── isTokenValid ────────────────────────────────────────────────────────

    @Nested
    @DisplayName("isTokenValid")
    class IsTokenValid {

        @Test
        @DisplayName("Retorna true para un token vigente y bien firmado")
        void isTokenValid_tokenValido_retornaTrue() {
            String token = buildToken("admin@cordillera.cl", "ADMIN", 1L, 60_000);
            assertThat(jwtService.isTokenValid(token)).isTrue();
        }

        @Test
        @DisplayName("Retorna false para un token expirado")
        void isTokenValid_tokenExpirado_retornaFalse() {
            String token = buildExpiredToken("admin@cordillera.cl");
            assertThat(jwtService.isTokenValid(token)).isFalse();
        }

        @Test
        @DisplayName("Retorna false para un token con firma inválida")
        void isTokenValid_firmaInvalida_retornaFalse() {
            String tokenFalsificado = "eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJoYWNrZXIifQ.firma_falsa";
            assertThat(jwtService.isTokenValid(tokenFalsificado)).isFalse();
        }

        @Test
        @DisplayName("Retorna false para un token malformado")
        void isTokenValid_tokenMalformado_retornaFalse() {
            assertThat(jwtService.isTokenValid("esto.no.es.un.jwt")).isFalse();
        }
    }
}