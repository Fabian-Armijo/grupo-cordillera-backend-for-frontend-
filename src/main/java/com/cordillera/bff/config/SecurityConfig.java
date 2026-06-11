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
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
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
                .addPathPatterns("/api/**")
                .excludePathPatterns("/api/auth/login", "/api/auth/register", "/api/auth/**");
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                // 🌐 1. CORS CONFIGURADO E INYECTADO EN PRIMER LUGAR DE LA CADENA
                .cors(cors -> cors.configurationSource(request -> {
                    CorsConfiguration config = new CorsConfiguration();
                    config.setAllowedOrigins(List.of("http://localhost:5173"));
                    config.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS"));
                    config.setAllowedHeaders(Arrays.asList("Authorization", "Content-Type", "Cache-Control"));
                    config.setExposedHeaders(List.of("Set-Cookie")); // 👈 Exponemos la cookie explícitamente
                    config.setAllowCredentials(true); // 👈 Requerido para HttpOnly Cookies
                    return config;
                }))

                // 2. Deshabilitamos CSRF (Ya que usamos tokens/BFF stateless)
                .csrf(csrf -> csrf.disable())

                // 3. Gestión de sesión sin estado
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

                // 4. Matriz de accesos
                // Reemplaza la línea de rutas públicas en tu SecurityConfig.java por esta variante:
                .authorizeHttpRequests(auth -> auth
                        // 🔓 Abrimos explícitamente cada combinación posible para eliminar el bloqueo automático
                        .requestMatchers("/api/auth/login", "/api/auth/login/", "/api/auth/**").permitAll()
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll() // 👈 Obligatorio para asegurar que no filtre preflights
                        .requestMatchers(HttpMethod.GET, "/api/sucursales").permitAll()

                        // ... el resto de tus rutas protegidas abajo queda igual ...

                        // Módulos Corporativos del Grupo Cordillera
                        .requestMatchers(HttpMethod.POST, "/api/kpi/definiciones/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.POST, "/api/kpi/metricas/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.GET, "/api/kpi/**").hasAnyRole("ADMIN", "GERENTE")
                        .requestMatchers(HttpMethod.POST, "/api/reportes/emitir").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.GET, "/api/reportes/historial").hasAnyRole("ADMIN", "GERENTE")
                        .requestMatchers("/api/stock/**").hasAnyRole("ADMIN", "GERENTE", "USUARIO")
                        .requestMatchers("/api/productos/**").hasAnyRole("ADMIN", "GERENTE", "USUARIO")
                        .requestMatchers("/api/categorias/**").hasAnyRole("ADMIN", "GERENTE", "USUARIO")
                        .requestMatchers(HttpMethod.POST, "/api/compras/procesar").hasRole("USUARIO")
                        .requestMatchers(HttpMethod.POST, "/api/ventas/anular/**").hasAnyRole("ADMIN", "GERENTE")
                        .requestMatchers("/api/ventas/**").hasAnyRole("ADMIN", "GERENTE", "USUARIO")

                        .anyRequest().authenticated()
                );

        // 5. Inyectamos tu filtro JWT corregido antes del nativo de Spring
        http.addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}