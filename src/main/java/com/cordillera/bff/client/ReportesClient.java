package com.cordillera.bff.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@FeignClient(name = "ms-reportes", url = "http://localhost:8089/api/reportes") // <-- Ajusta este puerto
public interface ReportesClient {

    @GetMapping("/historial")
    List<Map<String, Object>> obtenerHistorial();

    @GetMapping("/historial/{id}/descargar")
    byte[] descargarReporteAntiguo(@PathVariable("id") Long id);

    @GetMapping("/descargar")
    byte[] generarYDescargarReporte(@RequestParam("kpiId") Long kpiId,
                                    @RequestParam("sucursalId") Long sucursalId,
                                    @RequestParam("periodo") String periodo);

    @PostMapping("/enviar")
    String enviarReportePorCorreo(@RequestParam("kpiId") Long kpiId,
                                  @RequestParam("sucursalId") Long sucursalId,
                                  @RequestParam("periodo") String periodo,
                                  @RequestParam("correoDestino") String correoDestino);

    @GetMapping("/previsualizar")
    byte[] obtenerUrlPrevisualizacion(@RequestParam("kpiId") Long kpiId,
                                      @RequestParam("sucursalId") Long sucursalId,
                                      @RequestParam("periodo") String periodo);
}