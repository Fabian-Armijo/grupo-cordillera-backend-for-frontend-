package com.cordillera.bff.controller;

import com.cordillera.bff.dto.RespuestaResilienteDto; // 👈 Importamos nuestro sobre
import com.cordillera.bff.service.KpiBffService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/bff/kpis") // La ruta base que espera el API Gateway
public class KpiBFFController {

    @Autowired
    private KpiBffService kpiBffService;

    @GetMapping("/dashboard") // El endpoint final que completa la URL
    public ResponseEntity<List<Map<String, Object>>> obtenerDashboard() {
        // Llamamos al cerebro (Service) que acabamos de programar
        List<Map<String, Object>> dashboardData = kpiBffService.getKpisForDashboard();

        // Devolvemos el JSON con un código 200 OK
        return ResponseEntity.ok(dashboardData);
    }
    @PostMapping("/definiciones")
    public ResponseEntity<Map<String, Object>> crearDefinicion(@RequestBody Map<String, Object> definicion) {
        // kpiBffService no es estrictamente necesario aquí si solo es pasarela, llamamos al cliente directo
        return ResponseEntity.ok(kpiBffService.crearDefinicion(definicion));
    }

    @PostMapping("/metricas")
    public ResponseEntity<Map<String, Object>> crearMetrica(@RequestBody Map<String, Object> metrica) {
        return ResponseEntity.ok(kpiBffService.crearMetrica(metrica));
    }
}