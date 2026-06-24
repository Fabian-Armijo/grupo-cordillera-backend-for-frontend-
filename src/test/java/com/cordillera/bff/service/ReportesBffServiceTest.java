package com.cordillera.bff.service;

import com.cordillera.bff.client.ReportesClient;
import com.cordillera.bff.client.fallback.SucursalFallback;
import com.cordillera.bff.dto.SucursalResponseDto;
import com.cordillera.bff.service.ReportesBffService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ReportesBffService y SucursalFallback – Tests unitarios")
class ReportesBffServiceTest {

    @Mock
    private ReportesClient reportesClient;

    @InjectMocks
    private ReportesBffService reportesBffService;

    // ─── obtenerHistorial ─────────────────────────────────────────────────────

    @Nested
    @DisplayName("obtenerHistorial")
    class ObtenerHistorial {

        @Test
        @DisplayName("Delega la llamada al ReportesClient con rol y sucursal")
        void obtenerHistorial_delegaAReportesClient() {
            List<Map<String, Object>> historial = List.of(
                    Map.of("id", 1, "periodo", "2026-06"),
                    Map.of("id", 2, "periodo", "2026-05")
            );

            when(reportesClient.obtenerHistorial("ADMIN", 1L)).thenReturn(historial);

            List<Map<String, Object>> resultado =
                    reportesBffService.obtenerHistorial("ADMIN", 1L);

            assertThat(resultado).hasSize(2);
            assertThat(resultado.get(0).get("periodo")).isEqualTo("2026-06");
            verify(reportesClient, times(1)).obtenerHistorial("ADMIN", 1L);
        }

        @Test
        @DisplayName("Retorna lista vacía si el cliente no devuelve historial")
        void obtenerHistorial_sinDatos_listaVacia() {
            when(reportesClient.obtenerHistorial("GERENTE", 2L)).thenReturn(List.of());

            List<Map<String, Object>> resultado =
                    reportesBffService.obtenerHistorial("GERENTE", 2L);

            assertThat(resultado).isEmpty();
        }
    }

    // ─── descargarReporteAntiguo ──────────────────────────────────────────────

    @Nested
    @DisplayName("descargarReporteAntiguo")
    class DescargarReporteAntiguo {

        @Test
        @DisplayName("Retorna los bytes del PDF delegando al ReportesClient")
        void descargarReporteAntiguo_retornaBytes() {
            byte[] pdfBytes = new byte[]{1, 2, 3, 4, 5};
            when(reportesClient.descargarReporteAntiguo(7L)).thenReturn(pdfBytes);

            byte[] resultado = reportesBffService.descargarReporteAntiguo(7L);

            assertThat(resultado).isEqualTo(pdfBytes);
            verify(reportesClient, times(1)).descargarReporteAntiguo(7L);
        }
    }

    // ─── generarYDescargarReporte ─────────────────────────────────────────────

    @Nested
    @DisplayName("generarYDescargarReporte")
    class GenerarYDescargarReporte {

        @Test
        @DisplayName("Delega todos los parámetros al ReportesClient correctamente")
        void generarReporte_delegaTodosLosParametros() {
            byte[] pdfBytes = new byte[]{10, 20, 30};
            when(reportesClient.generarYDescargarReporte(1L, 2L, "2026-06", "ADMIN", 2L))
                    .thenReturn(pdfBytes);

            byte[] resultado = reportesBffService.generarYDescargarReporte(
                    1L, 2L, "2026-06", "ADMIN", 2L);

            assertThat(resultado).isEqualTo(pdfBytes);
            verify(reportesClient, times(1))
                    .generarYDescargarReporte(1L, 2L, "2026-06", "ADMIN", 2L);
        }
    }

    // ─── enviarReportePorCorreo ───────────────────────────────────────────────

    @Nested
    @DisplayName("enviarReportePorCorreo")
    class EnviarReportePorCorreo {

        @Test
        @DisplayName("Delega el envío al ReportesClient y retorna la confirmación")
        void enviarReporte_delegaYRetornaConfirmacion() {
            when(reportesClient.enviarReportePorCorreo(
                    1L, 3L, "2026-06", "gerente@cordillera.cl", "GERENTE", 3L))
                    .thenReturn("Reporte enviado exitosamente");

            String resultado = reportesBffService.enviarReportePorCorreo(
                    1L, 3L, "2026-06", "gerente@cordillera.cl", "GERENTE", 3L);

            assertThat(resultado).isEqualTo("Reporte enviado exitosamente");
        }
    }

    // ─── obtenerUrlPrevisualizacion ───────────────────────────────────────────

    @Nested
    @DisplayName("obtenerUrlPrevisualizacion")
    class ObtenerUrlPrevisualizacion {

        @Test
        @DisplayName("Delega al ReportesClient y retorna el PDF de previsualización")
        void obtenerPrevisualizacion_delegaAReportesClient() {
            byte[] pdfBytes = new byte[]{50, 60, 70};
            when(reportesClient.obtenerUrlPrevisualizacion(2L, 1L, "2026-05", "ADMIN", 1L))
                    .thenReturn(pdfBytes);

            byte[] resultado = reportesBffService.obtenerUrlPrevisualizacion(
                    2L, 1L, "2026-05", "ADMIN", 1L);

            assertThat(resultado).isEqualTo(pdfBytes);
            verify(reportesClient, times(1))
                    .obtenerUrlPrevisualizacion(2L, 1L, "2026-05", "ADMIN", 1L);
        }
    }

    // ─── listarKpisParaSelector ───────────────────────────────────────────────

    @Nested
    @DisplayName("listarKpisParaSelector")
    class ListarKpisParaSelector {

        @Test
        @DisplayName("Retorna la lista dinámica desde el ReportesClient cuando está disponible")
        void listarKpis_clienteDisponible_retornaListaDinamica() {
            List<Map<String, Object>> kpis = List.of(
                    Map.of("id", 1, "nombre", "Ventas Totales"),
                    Map.of("id", 2, "nombre", "Stock Disponible"),
                    Map.of("id", 3, "nombre", "Transacciones")
            );

            when(reportesClient.obtenerKpisDisponibles()).thenReturn(kpis);

            List<Map<String, Object>> resultado = reportesBffService.listarKpisParaSelector();

            assertThat(resultado).hasSize(3);
            assertThat(resultado.get(0).get("nombre")).isEqualTo("Ventas Totales");
        }

        @Test
        @DisplayName("Retorna los 2 KPIs por defecto cuando el ReportesClient lanza excepción")
        void listarKpis_clienteFalla_retornaValoresPorDefecto() {
            when(reportesClient.obtenerKpisDisponibles())
                    .thenThrow(new RuntimeException("ms-reportes no disponible"));

            List<Map<String, Object>> resultado = reportesBffService.listarKpisParaSelector();

            assertThat(resultado).hasSize(2);

            List<Object> nombres = resultado.stream()
                    .map(m -> m.get("nombre"))
                    .toList();

            assertThat(nombres).contains("Monto Total de Ventas", "Unidades Totales Vendidas");
        }

        @Test
        @DisplayName("Los KPIs por defecto tienen los IDs 1 y 2")
        void listarKpis_clienteFalla_losDefaultsTienenIdsCorrectos() {
            when(reportesClient.obtenerKpisDisponibles())
                    .thenThrow(new RuntimeException("timeout"));

            List<Map<String, Object>> resultado = reportesBffService.listarKpisParaSelector();

            List<Object> ids = resultado.stream().map(m -> m.get("id")).toList();
            assertThat(ids).contains(1, 2);
        }
    }

    // ─── SucursalFallback ─────────────────────────────────────────────────────

    @Nested
    @DisplayName("SucursalFallback – Circuit Breaker")
    class SucursalFallbackTest {

        private final SucursalFallback fallback = new SucursalFallback();

        @Test
        @DisplayName("obtenerPorId retorna DTO de fallback con 'activa = false'")
        void obtenerPorId_retornaDtoFallback() {
            SucursalResponseDto resultado = fallback.obtenerPorId(5L);

            assertThat(resultado.getId()).isEqualTo(5L);
            assertThat(resultado.getNombre()).isEqualTo("Servicio de Sucursales no disponible");
            assertThat(resultado.getCodigo()).isEqualTo("N/A");
            assertThat(resultado.getDireccion()).isEqualTo("Temporalmente fuera de línea");
            assertThat(resultado.getActiva()).isFalse();
        }

        @Test
        @DisplayName("listarTodas retorna lista con un DTO de fallback")
        void listarTodas_retornaListaConFallback() {
            List<SucursalResponseDto> resultado = fallback.listarTodas();

            assertThat(resultado).hasSize(1);
            assertThat(resultado.get(0).getNombre()).isEqualTo("SERVICIO NO DISPONIBLE");
            assertThat(resultado.get(0).getDireccion()).isEqualTo("Intente más tarde");
        }
    }
}