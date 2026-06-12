package com.cordillera.bff.service;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.security.Key;
import java.util.Date;
import java.util.List;
import java.util.function.Function;

@Service
public class JwtService {

    // 🌟 Es buena práctica guardar tu clave secreta en el application.properties o application.yml
    @Value("${application.security.jwt.secret-key:404E635266556A586E3272357538782F413F4428472B4B6250645367566B5970}")
    private String secretKey;

    /**
     * Extrae el nombre de usuario (subject) del token.
     */
    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    /**
     * 🌟 CRUCIAL: Extrae el rol asignado que guardamos en los Claims del token.
     * En tu microservicio de Autenticación, al crear el token debiste haber guardado
     * el rol usando algo como: claims.put("role", usuario.getRol().name());
     */
    public String extractRole(String token) {
        final Claims claims = extractAllClaims(token);

        // 1. Buscamos la caja en plural "roles" que mandó el ms-autenticación
        List<?> roles = claims.get("roles", List.class);

        if (roles != null && !roles.isEmpty()) {
            String rol = roles.get(0).toString(); // Esto va a ser "ROLE_ADMIN"

            // 🧼 Si ya empieza con "ROLE_", se lo quitamos temporalmente
            // porque tu filtro JwtAuthenticationFilter ya le agrega el "ROLE_" manualmente.
            if (rol.startsWith("ROLE_")) {
                return rol.replace("ROLE_", ""); // Devuelve solo "ADMIN"
            }
            return rol;
        }

        return "USER"; // Rol por defecto si pasa algo raro
    }

    /**
     * Valida si el token es estructuralmente correcto y no ha expirado.
     */
    public boolean isTokenValid(String token) {
        try {
            return !isTokenExpired(token);
        } catch (Exception e) {
            return false; // Token alterado, firma inválida o malformado
        }
    }

    private boolean isTokenExpired(String token) {
        return extractExpiration(token).before(new Date());
    }

    private Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }

    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    private Claims extractAllClaims(String token) {
        return Jwts
                .parserBuilder()
                .setSigningKey(getSignInKey())
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    // Cambia esto en el JwtService.java de tu BFF:
    private Key getSignInKey() {

        byte[] keyBytes = secretKey.getBytes();
        return Keys.hmacShaKeyFor(keyBytes);
    }

    public Long extractSucursalId(String token) {
        try {
            final Claims claims = extractAllClaims(token);
            // Intentamos leerlo como Long, si falla lo pasamos a String y luego a Long
            Object sucursal = claims.get("sucursalId");
            if (sucursal == null) {
                sucursal = claims.get("sucursal"); // Por si acaso se guardó sin el "Id"
            }
            return sucursal != null ? Long.valueOf(sucursal.toString()) : null;
        } catch (Exception e) {
            System.out.println("[⚠️ JWT] Error extrayendo sucursalId: " + e.getMessage());
            return null;
        }
    }
}