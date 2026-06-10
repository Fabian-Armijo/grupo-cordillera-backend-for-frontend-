package com.cordillera.bff.controller;

import com.cordillera.bff.dto.VentaRequestDto;
import com.cordillera.bff.dto.VentaResponseDto;
import com.cordillera.bff.dto.SucursalResponseDto;
import com.cordillera.bff.service.VentasBffService;
import com.cordillera.bff.service.KpiBffService; // 🚀 1. Inyectamos tu servicio de KPIs
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/bff/ventas")
public class VentaBffController {

    @Autowired
    private VentasBffService vBffService;

    @Autowired
    private KpiBffService kpiBffService; // 🚀 2. Declaramos el puente hacia el servicio de KPIs

    // Responde a GET http://localhost:8086/bff/ventas
    @GetMapping
    public ResponseEntity<List<VentaResponseDto>> listarTodas() {
        return ResponseEntity.ok(vBffService.listarTodasLasVentas());
    }

    @PostMapping("/confirmar")
    public ResponseEntity<VentaResponseDto> confirmarVenta(@RequestBody VentaRequestDto request) {
        // A. Primero se procesa y guarda la venta en el MS-Ventas de forma normal
        VentaResponseDto ventaProcesada = vBffService.procesarVenta(request);

        // B. 🚀 EL AUTOMATISMO: Si la venta se procesó sin errores, actualizamos el KPI
        if (ventaProcesada != null) {
            try {
                // Preparamos la estructura Map idéntica al JSON que espera tu MS-KPIs
                Map<String, Object> metricaPayload = new HashMap<>();

                Map<String, Object> definicionId = new HashMap<>();
                // 🎯 Asegúrate de que el ID 1 corresponda al KPI "Ventas Totales" que creaste en tu frontend
                definicionId.put("id", 1);

                metricaPayload.put("definicion", definicionId);
                metricaPayload.put("sucursalId", request.getSucursalId()); // Le pasamos la sucursal de la venta
                metricaPayload.put("valorActual", 1); // ➕ Incremento automático de +1 por esta boleta

                // Enviamos el impacto al MS-KPIs usando el método pasarela que ya tienes listo
                kpiBffService.crearMetrica(metricaPayload);
                System.out.println("✅ BFF Orquestador: KPI de ventas incrementado de forma automática (+1)");

            } catch (Exception e) {
                // Con este try-catch protegemos el flujo: si el MS-KPIs llega a fallar,
                // la venta en React NO se caerá ni le mostrará error al cliente.
                System.err.println("❌ Error al intentar actualizar el KPI desde el BFF de Ventas: " + e.getMessage());
            }
        }

        return ResponseEntity.ok(ventaProcesada);
    }

    @GetMapping("/sucursales-activas")
    public ResponseEntity<List<SucursalResponseDto>> getSucursales() {
        return ResponseEntity.ok(vBffService.obtenerSucursalesParaVenta());
    }
}