package com.cordillera.bff.config;

import feign.RequestInterceptor;
import feign.RequestTemplate;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Component
public class FeignInterceptorConfig implements RequestInterceptor {

    @Override
    public void apply(RequestTemplate template) {

        String authHeader = null;
        String sucursalHeader = null;

        try {

            ServletRequestAttributes attributes =
                    (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();

            if (attributes == null) {

                System.out.println("[❌ FEIGN] No existe RequestContext activo");
                return;
            }

            HttpServletRequest request = attributes.getRequest();

            authHeader = request.getHeader("Authorization");
            sucursalHeader = request.getHeader("X-Sucursal-Id");

            if (authHeader == null || authHeader.isBlank()) {

                System.out.println("[❌ FEIGN] Authorization no encontrado");
                return;
            }

            System.out.println("======================================");
            System.out.println("[🚀 FEIGN INTERCEPTOR]");
            System.out.println("Authorization: " + authHeader);

            if (sucursalHeader != null) {
                System.out.println("Sucursal Header: " + sucursalHeader);
            }

            System.out.println("======================================");

            template.header("Authorization", authHeader);

            if (sucursalHeader != null &&
                    !sucursalHeader.isBlank()) {

                template.header(
                        "X-Sucursal-Id",
                        sucursalHeader
                );
            }

        } catch (Exception e) {

            System.out.println(
                    "[❌ FEIGN] Error propagando headers: "
                            + e.getMessage()
            );
        }
    }
}