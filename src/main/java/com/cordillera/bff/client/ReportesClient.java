package com.cordillera.bff.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@FeignClient(name = "ms-reportes", url = "http://localhost:8089/api/reportes")
public interface ReportesClient {

    // 🎯 CORREGIDO: Ahora propaga el rol y sucursal autenticada para aislar el historial del Gerente
    @GetMapping("/historial")
    List<Map<String, Object>> obtenerHistorial(
            @RequestHeader("X-User-Role") String rol,
            @RequestHeader("X-Sucursal-Id") Long sucursalAutenticada
    );

    @GetMapping("/historial/{id}/descargar")
    byte[] descargarReporteAntiguo(@PathVariable("id") Long id);

    // 🎯 CORREGIDO: Propaga cabeceras para descargas en tiempo real
    @GetMapping("/descargar")
    byte[] generarYDescargarReporte(
            @RequestParam("kpiId") Long kpiId,
            @RequestParam("sucursalId") Long sucursalId,
            @RequestParam("periodo") String periodo,
            @RequestHeader("X-User-Role") String rol,
            @RequestHeader("X-Sucursal-Id") Long sucursalAutenticada
    );

    @PostMapping("/enviar")
    String enviarReportePorCorreo(
            @RequestParam("kpiId") Long kpiId,
            @RequestParam("sucursalId") Long sucursalId,
            @RequestParam("periodo") String periodo,
            @RequestParam("correoDestino") String correoDestino,
            @RequestHeader("X-User-Role") String rol,
            @RequestHeader("X-Sucursal-Id") Long sucursalAutenticada
    );

    // 🎯 CORREGIDO: Propaga cabeceras para la previsualización interactiva
    @GetMapping("/previsualizar")
    byte[] obtenerUrlPrevisualizacion(
            @RequestParam("kpiId") Long kpiId,
            @RequestParam("sucursalId") Long sucursalId,
            @RequestParam("periodo") String periodo,
            @RequestHeader("X-User-Role") String rol,
            @RequestHeader("X-Sucursal-Id") Long sucursalAutenticada
    );

    // 🌟 ENLACE SOLUCIONADO: Mapea el catálogo de KPIs dinámicos para el selector del Frontend
    @GetMapping("/kpis-disponibles")
    List<Map<String, Object>> obtenerKpisDisponibles();
}