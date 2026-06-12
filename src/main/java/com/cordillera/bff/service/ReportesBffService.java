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

    public List<Map<String, Object>> obtenerHistorial(String rol, Long sucursalAutenticada) {
        return reportesClient.obtenerHistorial(rol, sucursalAutenticada);
    }

    public byte[] descargarReporteAntiguo(Long id) {
        return reportesClient.descargarReporteAntiguo(id);
    }

    public byte[] generarYDescargarReporte(Long kpiId, Long sucursalId, String periodo, String rol, Long sucursalAutenticada) {
        return reportesClient.generarYDescargarReporte(kpiId, sucursalId, periodo, rol, sucursalAutenticada);
    }

    public String enviarReportePorCorreo(Long kpiId, Long sucursalId, String periodo, String correoDestino, String rol, Long sucursalAutenticada) {
        return reportesClient.enviarReportePorCorreo(kpiId, sucursalId, periodo, correoDestino, rol, sucursalAutenticada);
    }

    public byte[] obtenerUrlPrevisualizacion(Long kpiId, Long sucursalId, String periodo, String rol, Long sucursalAutenticada) {
        return reportesClient.obtenerUrlPrevisualizacion(kpiId, sucursalId, periodo, rol, sucursalAutenticada);
    }

    // 🎯 NUEVOS CAMBIOS: Agregamos el método que le falta al controlador
    public List<Map<String, Object>> listarKpisParaSelector() {
        try {
            // Le pedimos la lista al cliente Feign que se comunica con el microservicio
            return reportesClient.obtenerKpisDisponibles();
        } catch (Exception e) {
            // 🛡️ Salvavidas: Si el microservicio de KPIs no responde, el BFF no se cae
            // y le manda al Front las dos opciones por defecto para que siga operando.
            System.err.println("⚠️ Alerta en BFF: No se pudo conectar al microservicio de KPIs. Usando valores por defecto. Motivo: " + e.getMessage());

            return List.of(
                    Map.of("id", 1, "nombre", "Monto Total de Ventas"),
                    Map.of("id", 2, "nombre", "Unidades Totales Vendidas")
            );
        }
    }
}