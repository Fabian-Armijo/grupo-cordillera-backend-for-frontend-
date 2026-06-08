package com.cordillera.bff.controller;

import com.cordillera.bff.service.KpiBffService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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
}