package com.cordillera.bff.service;


import com.cordillera.bff.client.ReportesClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
public class ReportesBffService {

    @Autowired
    private ReportesClient reportesClient;

    public List<Map<String, Object>> obtenerHistorial() {
        return reportesClient.obtenerHistorial();
    }

    public byte[] descargarReporteAntiguo(Long id) {
        return reportesClient.descargarReporteAntiguo(id);
    }

    public byte[] generarYDescargarReporte(Long kpiId, Long sucursalId, String periodo) {
        return reportesClient.generarYDescargarReporte(kpiId, sucursalId, periodo);
    }

    public String enviarReportePorCorreo(Long kpiId, Long sucursalId, String periodo, String correoDestino) {
        return reportesClient.enviarReportePorCorreo(kpiId, sucursalId, periodo, correoDestino);
    }

    public byte[] obtenerUrlPrevisualizacion(Long kpiId, Long sucursalId, String periodo) {
        return reportesClient.obtenerUrlPrevisualizacion(kpiId, sucursalId, periodo);
    }
}