package com.cordillera.bff.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.Arrays;
import java.util.List;

@Configuration
@EnableWebSecurity
public class SecurityConfig implements WebMvcConfigurer {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final TokenRelayInterceptor tokenRelayInterceptor;

    public SecurityConfig(JwtAuthenticationFilter jwtAuthenticationFilter,
                          TokenRelayInterceptor tokenRelayInterceptor) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
        this.tokenRelayInterceptor = tokenRelayInterceptor;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(tokenRelayInterceptor)
                .addPathPatterns("/api/**", "/bff/**")
                .excludePathPatterns("/api/auth/login", "/api/auth/login/", "/api/auth/**");
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                // 🌐 1. Configuración de CORS
                .cors(cors -> cors.configurationSource(request -> {
                    CorsConfiguration config = new CorsConfiguration();
                    config.setAllowedOrigins(List.of("http://localhost:5173", "http://localhost:3000"));
                    config.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS"));

                    // 🎯 CABECERAS CORREGIDAS: Agregamos "X-User-Role" manteniendo intactas las demás
                    config.setAllowedHeaders(Arrays.asList(
                            "Authorization",
                            "Content-Type",
                            "Cache-Control",
                            "X-Sucursal-Id",
                            "X-User-Role" // <-- Agregada para solucionar el bloqueo CORS Preflight
                    ));

                    config.setExposedHeaders(List.of("Set-Cookie"));
                    config.setAllowCredentials(true);
                    return config;
                }))

                // 2. Deshabilitamos CSRF
                .csrf(csrf -> csrf.disable())

                // 3. Gestión de sesión sin estado
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

                // 4. Matriz de accesos estandarizada usando roles
                .authorizeHttpRequests(auth -> auth
                        // 🔓 Rutas 100% Públicas (Login y Preflights)
                        .requestMatchers("/api/auth/login", "/api/auth/login/", "/api/auth/**").permitAll()
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/sucursales").permitAll()

                        // 📊 Módulo de Reportes y KPIs (Restringido solo a Altos Cargos)
                        // 🎯 ADAPTACIÓN: Agregamos "/api/reportes/**" porque React le pega a esa ruta ahora
                        .requestMatchers("/bff/reportes/**", "/api/reportes/**", "/api/kpi/**").hasAnyRole("ADMIN", "GERENTE")

                        // 🔓 ACCESO ACCESIBLE PARA TODOS LOS ROLES AUTENTICADOS (Cajero, Vendedor, Admin, etc.)
                        .requestMatchers(
                                "/bff/catalogo/lista",
                                "/bff/catalogo/categorias",
                                "/bff/catalogo/crear",
                                "/bff/ventas/**",
                                "/bff/productos/**",
                                "/api/sucursales/**",
                                "/api/stock/**",
                                "/api/productos/**"
                        ).authenticated()

                        // 🔒 Cualquier otra petición requiere autenticación por seguridad
                        .anyRequest().authenticated()
                );

        // 5. Inyectamos el filtro JWT corregido antes del nativo de Spring
        http.addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}