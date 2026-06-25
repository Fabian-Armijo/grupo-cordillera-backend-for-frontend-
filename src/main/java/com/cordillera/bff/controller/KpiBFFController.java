package com.cordillera.bff.controller;

import com.cordillera.bff.dto.RespuestaResilienteDto; // 👈 Importamos nuestro sobre
import com.cordillera.bff.service.KpiBffService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/bff/kpis")
public class KpiBFFController {

    @Autowired
    private KpiBffService kpiBffService;

    @GetMapping("/dashboard")
    public ResponseEntity<RespuestaResilienteDto<List<Map<String, Object>>>> getKpisForDashboard() {
        try {
            // 📦 Enviamos la data envuelta en nuestro DTO
            return ResponseEntity.ok(kpiBffService.getKpisForDashboard());
        } catch (Exception e) {
            // 🛡️ Fallback final de seguridad del Gateway
            System.err.println("❌ [GATEWAY-BFF] Falla crítica en endpoint de KPIs: " + e.getMessage());
            return ResponseEntity.internalServerError().body(new RespuestaResilienteDto<>(new ArrayList<>()));
        }
    }
}