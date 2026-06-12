package com.cordillera.bff.controller;

import com.cordillera.bff.service.ReportesBffService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/reportes") // 🎯 NOTA: Ajusté a '/api/reportes' porque tu JS de React le pega a esa ruta exacta
// 🛑 ELIMINADO TOTALMENTE: Quitamos cualquier rastro de @CrossOrigin de aquí para evitar la duplicación en el puerto 8086
public class ReportesBffController {

    @Autowired
    private ReportesBffService reportesBffService;

    @GetMapping("/historial")
    public ResponseEntity<List<Map<String, Object>>> obtenerHistorial(
            @RequestHeader(value = "X-User-Role", required = false) String rol,
            @RequestHeader(value = "X-Sucursal-Id", required = false) Long sucursalAutenticada) {

        return ResponseEntity.ok(reportesBffService.obtenerHistorial(rol, sucursalAutenticada));
    }

    @GetMapping(value = "/historial/{id}/descargar", produces = MediaType.APPLICATION_PDF_VALUE)
    public ResponseEntity<byte[]> descargarReporteAntiguo(@PathVariable Long id) {
        byte[] pdf = reportesBffService.descargarReporteAntiguo(id);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"reporte_antiguo_" + id + ".pdf\"")
                .body(pdf);
    }

    @GetMapping(value = "/descargar", produces = MediaType.APPLICATION_PDF_VALUE)
    public ResponseEntity<byte[]> generarYDescargarReporte(
            @RequestParam Long kpiId,
            @RequestParam Long sucursalId,
            @RequestParam String periodo,
            @RequestHeader(value = "X-User-Role", required = false) String rol,
            @RequestHeader(value = "X-Sucursal-Id", required = false) Long sucursalAutenticada) {

        byte[] pdf = reportesBffService.generarYDescargarReporte(kpiId, sucursalId, periodo, rol, sucursalAutenticada);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"reporte_" + periodo + ".pdf\"")
                .body(pdf);
    }

    @PostMapping("/enviar")
    public ResponseEntity<String> enviarReportePorCorreo(
            @RequestParam Long kpiId,
            @RequestParam Long sucursalId,
            @RequestParam String periodo,
            @RequestParam String correoDestino,
            @RequestHeader(value = "X-User-Role", required = false) String rol,
            @RequestHeader(value = "X-Sucursal-Id", required = false) Long sucursalAutenticada) {

        return ResponseEntity.ok(reportesBffService.enviarReportePorCorreo(kpiId, sucursalId, periodo, correoDestino, rol, sucursalAutenticada));
    }

    @GetMapping(value = "/previsualizar", produces = MediaType.APPLICATION_PDF_VALUE)
    public ResponseEntity<byte[]> obtenerUrlPrevisualizacion(
            @RequestParam Long kpiId,
            @RequestParam Long sucursalId,
            @RequestParam String periodo,
            @RequestHeader(value = "X-User-Role", required = false) String rol,
            @RequestHeader(value = "X-Sucursal-Id", required = false) Long sucursalAutenticada) {

        byte[] pdf = reportesBffService.obtenerUrlPrevisualizacion(kpiId, sucursalId, periodo, rol, sucursalAutenticada);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"previsualizacion.pdf\"")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdf);
    }
    @GetMapping("/kpis-disponibles")
    public ResponseEntity<List<Map<String, Object>>> obtenerKpisDisponibles() {
        // Llama al servicio del BFF para obtener el catálogo dinámico de ms-kpi
        return ResponseEntity.ok(reportesBffService.listarKpisParaSelector());
    }
}