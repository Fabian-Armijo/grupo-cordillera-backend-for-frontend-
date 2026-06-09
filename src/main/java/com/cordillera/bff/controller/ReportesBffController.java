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
@RequestMapping("/bff/reportes")
public class ReportesBffController {

    @Autowired
    private ReportesBffService reportesBffService;

    @GetMapping("/historial")
    public ResponseEntity<List<Map<String, Object>>> obtenerHistorial() {
        return ResponseEntity.ok(reportesBffService.obtenerHistorial());
    }

    @GetMapping(value = "/historial/{id}/descargar", produces = MediaType.APPLICATION_PDF_VALUE)
    public ResponseEntity<byte[]> descargarReporteAntiguo(@PathVariable Long id) {
        byte[] pdf = reportesBffService.descargarReporteAntiguo(id);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"reporte_antiguo_" + id + ".pdf\"")
                .body(pdf);
    }

    @GetMapping(value = "/descargar", produces = MediaType.APPLICATION_PDF_VALUE)
    public ResponseEntity<byte[]> generarYDescargarReporte(@RequestParam Long kpiId,
                                                           @RequestParam Long sucursalId,
                                                           @RequestParam String periodo) {
        byte[] pdf = reportesBffService.generarYDescargarReporte(kpiId, sucursalId, periodo);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"reporte_" + periodo + ".pdf\"")
                .body(pdf);
    }

    @PostMapping("/enviar")
    public ResponseEntity<String> enviarReportePorCorreo(@RequestParam Long kpiId,
                                                         @RequestParam Long sucursalId,
                                                         @RequestParam String periodo,
                                                         @RequestParam String correoDestino) {
        return ResponseEntity.ok(reportesBffService.enviarReportePorCorreo(kpiId, sucursalId, periodo, correoDestino));
    }

    @GetMapping(value = "/previsualizar", produces = MediaType.APPLICATION_PDF_VALUE)
    public ResponseEntity<byte[]> obtenerUrlPrevisualizacion(@RequestParam Long kpiId,
                                                             @RequestParam Long sucursalId,
                                                             @RequestParam String periodo) {
        byte[] pdf = reportesBffService.obtenerUrlPrevisualizacion(kpiId, sucursalId, periodo);
        return ResponseEntity.ok(pdf);
    }
}
