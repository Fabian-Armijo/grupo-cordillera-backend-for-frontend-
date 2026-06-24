package com.cordillera.bff.controller;

import com.cordillera.bff.service.ReportesBffService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReportesBffControllerTest {

    @Mock
    private ReportesBffService reportesBffService;

    @InjectMocks
    private ReportesBffController reportesBffController;

    @Test
    void obtenerHistorial_DeberiaRetornarListaHistorial() {
        // Arrange
        String rol = "ADMIN";
        Long sucursalId = 1L;

        List<Map<String, Object>> historial = List.of(
                Map.of("id", 1L, "nombre", "Reporte 1")
        );

        when(reportesBffService.obtenerHistorial(rol, sucursalId))
                .thenReturn(historial);

        // Act
        ResponseEntity<List<Map<String, Object>>> response =
                reportesBffController.obtenerHistorial(rol, sucursalId);

        // Assert
        assertEquals(200, response.getStatusCode().value());
        assertEquals(historial, response.getBody());

        verify(reportesBffService).obtenerHistorial(rol, sucursalId);
    }

    @Test
    void descargarReporteAntiguo_DeberiaRetornarPdf() {
        // Arrange
        Long id = 10L;
        byte[] pdf = "PDF".getBytes();

        when(reportesBffService.descargarReporteAntiguo(id))
                .thenReturn(pdf);

        // Act
        ResponseEntity<byte[]> response =
                reportesBffController.descargarReporteAntiguo(id);

        // Assert
        assertEquals(200, response.getStatusCode().value());
        assertArrayEquals(pdf, response.getBody());

        String contentDisposition =
                response.getHeaders().getFirst(HttpHeaders.CONTENT_DISPOSITION);

        assertTrue(contentDisposition.contains("reporte_antiguo_10.pdf"));

        verify(reportesBffService).descargarReporteAntiguo(id);
    }

    @Test
    void generarYDescargarReporte_DeberiaRetornarPdf() {
        // Arrange
        Long kpiId = 1L;
        Long sucursalId = 2L;
        String periodo = "2025-01";
        String rol = "ADMIN";
        Long sucursalAuth = 2L;

        byte[] pdf = "PDF_GENERADO".getBytes();

        when(reportesBffService.generarYDescargarReporte(
                kpiId, sucursalId, periodo, rol, sucursalAuth))
                .thenReturn(pdf);

        // Act
        ResponseEntity<byte[]> response =
                reportesBffController.generarYDescargarReporte(
                        kpiId,
                        sucursalId,
                        periodo,
                        rol,
                        sucursalAuth
                );

        // Assert
        assertEquals(200, response.getStatusCode().value());
        assertArrayEquals(pdf, response.getBody());

        String header =
                response.getHeaders().getFirst(HttpHeaders.CONTENT_DISPOSITION);

        assertTrue(header.contains("reporte_" + periodo + ".pdf"));

        verify(reportesBffService)
                .generarYDescargarReporte(
                        kpiId,
                        sucursalId,
                        periodo,
                        rol,
                        sucursalAuth
                );
    }

    @Test
    void enviarReportePorCorreo_DeberiaRetornarMensajeExitoso() {
        // Arrange
        Long kpiId = 1L;
        Long sucursalId = 2L;
        String periodo = "2025-01";
        String correo = "test@test.com";
        String rol = "ADMIN";
        Long sucursalAuth = 2L;

        String mensaje = "Correo enviado correctamente";

        when(reportesBffService.enviarReportePorCorreo(
                kpiId,
                sucursalId,
                periodo,
                correo,
                rol,
                sucursalAuth))
                .thenReturn(mensaje);

        // Act
        ResponseEntity<String> response =
                reportesBffController.enviarReportePorCorreo(
                        kpiId,
                        sucursalId,
                        periodo,
                        correo,
                        rol,
                        sucursalAuth
                );

        // Assert
        assertEquals(200, response.getStatusCode().value());
        assertEquals(mensaje, response.getBody());

        verify(reportesBffService)
                .enviarReportePorCorreo(
                        kpiId,
                        sucursalId,
                        periodo,
                        correo,
                        rol,
                        sucursalAuth
                );
    }

    @Test
    void obtenerUrlPrevisualizacion_DeberiaRetornarPdfInline() {
        // Arrange
        Long kpiId = 1L;
        Long sucursalId = 2L;
        String periodo = "2025-01";
        String rol = "ADMIN";
        Long sucursalAuth = 2L;

        byte[] pdf = "PREVIEW".getBytes();

        when(reportesBffService.obtenerUrlPrevisualizacion(
                kpiId,
                sucursalId,
                periodo,
                rol,
                sucursalAuth))
                .thenReturn(pdf);

        // Act
        ResponseEntity<byte[]> response =
                reportesBffController.obtenerUrlPrevisualizacion(
                        kpiId,
                        sucursalId,
                        periodo,
                        rol,
                        sucursalAuth
                );

        // Assert
        assertEquals(200, response.getStatusCode().value());
        assertArrayEquals(pdf, response.getBody());

        String header =
                response.getHeaders().getFirst(HttpHeaders.CONTENT_DISPOSITION);

        assertTrue(header.contains("previsualizacion.pdf"));

        verify(reportesBffService)
                .obtenerUrlPrevisualizacion(
                        kpiId,
                        sucursalId,
                        periodo,
                        rol,
                        sucursalAuth
                );
    }

    @Test
    void obtenerKpisDisponibles_DeberiaRetornarListadoKpis() {
        // Arrange
        List<Map<String, Object>> kpis = List.of(
                Map.of("id", 1L, "nombre", "Ventas"),
                Map.of("id", 2L, "nombre", "Satisfacción")
        );

        when(reportesBffService.listarKpisParaSelector())
                .thenReturn(kpis);

        // Act
        ResponseEntity<List<Map<String, Object>>> response =
                reportesBffController.obtenerKpisDisponibles();

        // Assert
        assertEquals(200, response.getStatusCode().value());
        assertEquals(kpis, response.getBody());

        verify(reportesBffService).listarKpisParaSelector();
    }
}