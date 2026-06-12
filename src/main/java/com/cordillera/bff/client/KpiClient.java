package com.cordillera.bff.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;

import java.util.List;
import java.util.Map;

@FeignClient(name = "kpi-client", url = "http://localhost:8087/api/kpi")
public interface KpiClient {

    @GetMapping("/definiciones")
    List<Map<String, Object>> obtenerTodosLosKpis();

    @GetMapping("/metricas/{id}")
    List<Map<String, Object>> obtenerMetricasPorKpi(@PathVariable("id") Long id);

    @PostMapping("/definiciones")
    Map<String, Object> crearDefinicion(@RequestBody Map<String, Object> definicion);

    @PostMapping("/metricas")
    Map<String, Object> crearMetrica(@RequestBody Map<String, Object> metrica);

    // 🎯 NUEVO ENDPOINT INYECTADO: Conecta con la acumulación transaccional de ms-kpi
    @PutMapping("/acumular")
    ResponseEntity<Void> acumularProgreso(
            @RequestParam("sucursalId") Long sucursalId,
            @RequestBody List<Map<String, Object>> productosVendidos
    );
}