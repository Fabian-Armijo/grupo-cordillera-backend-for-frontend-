package com.cordillera.bff.controller;



import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/stock")
public class StockController {

    @GetMapping("/sucursal/{sucursalId}")
    public ResponseEntity<?> obtenerStockPorSucursal(@PathVariable String sucursalId) {
        // 1. Obtenemos los detalles del usuario que hizo la petición HTTP
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String username = auth.getName();

        // Obtenemos el rol limpio evaluando las Authorities de Spring
        String userRole = auth.getAuthorities().stream()
                .map(granted -> granted.getAuthority())
                .findFirst()
                .orElse("");

        // 2. APLICAR REGLA DE NEGOCIO:
        // Si es un USUARIO operativo, validamos que no esté intentando ver otra sucursal
        if (userRole.equals("ROLE_USUARIO")) {
            String sucursalAsignadaAlUsuario = "1"; // En producción se consulta a la DB o viene en el JWT

            if (!sucursalId.equals(sucursalAsignadaAlUsuario)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body("🚫 Error de Auditoría: Tu usuario no pertenece a la sucursal " + sucursalId + ". No puedes consultar stocks ajenos.");
            }
        }

        // Si pasa la validación (o es ADMIN/GERENTE), procedemos a buscar los datos
        // Aquí iría la llamada al microservicio ms-stock:
        // List<StockDTO> stock = stockService.findBySucursal(sucursalId);

        return ResponseEntity.ok("Mapeo de existencias procesado con éxito para la sucursal " + sucursalId);
    }
}