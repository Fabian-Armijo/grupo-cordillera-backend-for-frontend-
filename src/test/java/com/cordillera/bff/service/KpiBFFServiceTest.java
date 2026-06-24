package com.cordillera.bff.service;

import com.cordillera.bff.client.KpiClient;
import com.cordillera.bff.client.SucursalClient;
import com.cordillera.bff.client.VentasClient;
import com.cordillera.bff.dto.SucursalResponseDto;
import com.cordillera.bff.dto.VentaResponseDto;
import com.cordillera.bff.service.KpiBffService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("KpiBffService – Tests unitarios")
class KpiBffServiceTest {

    @Mock
    private KpiClient kpiClient;

    @Mock
    private SucursalClient sucursalClient;

    @Mock
    private VentasClient ventasClient;

    @InjectMocks
    private KpiBffService kpiBffService;

    // ─── Helpers ─────────────────────────────────────────────────────────────

    private Map<String, Object> buildDefinicion(Long id, String nombre) {
        Map<String, Object> def = new HashMap<>();
        def.put("id", id);
        def.put("nombre", nombre);
        return def;
    }

    private Map<String, Object> buildMetrica(Long sucursalId, double valorActual) {
        Map<String, Object> m = new HashMap<>();
        m.put("sucursalId", sucursalId);
        m.put("valorActual", valorActual);
        return m;
    }

    private VentaResponseDto buildVenta(Double monto) {
        VentaResponseDto v = new VentaResponseDto();
        v.setMontoTotal(monto);
        return v;
    }

    // ─── getKpisForDashboard ─────────────────────────────────────────────────

    @Nested
    @DisplayName("getKpisForDashboard")
    class GetKpisForDashboard {

        @Test
        @DisplayName("KPI de ventas: calcula suma total desde VentasClient y agrega 'metricas'")
        void kpiDeVentas_acumulaSumaDesdeVentasClient() {
            Map<String, Object> defVentas = buildDefinicion(1L, "KPI Ventas Totales");

            when(kpiClient.obtenerTodosLosKpis()).thenReturn(List.of(defVentas));
            when(ventasClient.listarVentas(null, null, null)).thenReturn(
                    List.of(buildVenta(10_000.0), buildVenta(5_000.0))
            );

            List<Map<String, Object>> resultado = kpiBffService.getKpisForDashboard();

            assertThat(resultado).hasSize(1);

            @SuppressWarnings("unchecked")
            List<Map<String, Object>> metricas =
                    (List<Map<String, Object>>) resultado.get(0).get("metricas");

            assertThat(metricas).hasSize(1);
            assertThat(metricas.get(0).get("valorActual")).isEqualTo(15_000.0);
            assertThat(metricas.get(0).get("sucursalNombre")).isEqualTo("Global Consolidado");
        }

        @Test
        @DisplayName("KPI de ventas: solo llama a VentasClient una vez aunque haya múltiples KPIs de ventas")
        void kpiDeVentas_soloCalculaVentasUnaVez() {
            Map<String, Object> def1 = buildDefinicion(1L, "KPI Ventas Enero");
            Map<String, Object> def2 = buildDefinicion(2L, "Total de ventas mensuales");

            when(kpiClient.obtenerTodosLosKpis()).thenReturn(List.of(def1, def2));
            when(ventasClient.listarVentas(null, null, null))
                    .thenReturn(List.of(buildVenta(20_000.0)));

            kpiBffService.getKpisForDashboard();

            verify(ventasClient, times(1)).listarVentas(null, null, null);
        }

        @Test
        @DisplayName("KPI de ventas: si VentasClient lanza excepción, el valorActual queda en 0.0")
        void kpiDeVentas_ventasClientFalla_valorActualEsCero() {
            Map<String, Object> defVentas = buildDefinicion(1L, "ventas del mes");

            when(kpiClient.obtenerTodosLosKpis()).thenReturn(List.of(defVentas));
            when(ventasClient.listarVentas(null, null, null))
                    .thenThrow(new RuntimeException("ms-ventas no disponible"));

            List<Map<String, Object>> resultado = kpiBffService.getKpisForDashboard();

            @SuppressWarnings("unchecked")
            List<Map<String, Object>> metricas =
                    (List<Map<String, Object>>) resultado.get(0).get("metricas");

            assertThat(metricas.get(0).get("valorActual")).isEqualTo(0.0);
        }

        @Test
        @DisplayName("KPI no-ventas: consulta métricas por ID y enriquece con nombre de sucursal")
        void kpiNoVentas_enriqueceConNombreDeSucursal() {
            Map<String, Object> defStock = buildDefinicion(5L, "Stock Disponible");
            Map<String, Object> metrica = buildMetrica(2L, 300.0);

            when(kpiClient.obtenerTodosLosKpis()).thenReturn(List.of(defStock));
            when(kpiClient.obtenerMetricasPorKpi(5L)).thenReturn(List.of(metrica));

            SucursalResponseDto sucursal = SucursalResponseDto.builder()
                    .id(2L).nombre("Sucursal Santiago Centro").build();
            when(sucursalClient.obtenerPorId(2L)).thenReturn(sucursal);

            List<Map<String, Object>> resultado = kpiBffService.getKpisForDashboard();

            @SuppressWarnings("unchecked")
            List<Map<String, Object>> metricas =
                    (List<Map<String, Object>>) resultado.get(0).get("metricas");

            assertThat(metricas.get(0).get("sucursalNombre")).isEqualTo("Sucursal Santiago Centro");
        }

        @Test
        @DisplayName("KPI no-ventas: si SucursalClient falla, sucursalNombre queda como 'Sucursal Desconocida'")
        void kpiNoVentas_sucursalClientFalla_nombreDesconocido() {
            Map<String, Object> defStock = buildDefinicion(5L, "Transacciones");
            Map<String, Object> metrica = buildMetrica(99L, 10.0);

            when(kpiClient.obtenerTodosLosKpis()).thenReturn(List.of(defStock));
            when(kpiClient.obtenerMetricasPorKpi(5L)).thenReturn(List.of(metrica));
            when(sucursalClient.obtenerPorId(99L))
                    .thenThrow(new RuntimeException("Sucursal no encontrada"));

            List<Map<String, Object>> resultado = kpiBffService.getKpisForDashboard();

            @SuppressWarnings("unchecked")
            List<Map<String, Object>> metricas =
                    (List<Map<String, Object>>) resultado.get(0).get("metricas");

            assertThat(metricas.get(0).get("sucursalNombre")).isEqualTo("Sucursal Desconocida");
        }

        @Test
        @DisplayName("KPI no-ventas: métrica sin sucursalId agrega 'Dato Global'")
        void kpiNoVentas_sinSucursalId_agregaDatoGlobal() {
            Map<String, Object> defStock = buildDefinicion(3L, "Unidades Disponibles");
            Map<String, Object> metrica = new HashMap<>();
            metrica.put("valorActual", 500.0);
            // sucursalId = null intencionalmente

            when(kpiClient.obtenerTodosLosKpis()).thenReturn(List.of(defStock));
            when(kpiClient.obtenerMetricasPorKpi(3L)).thenReturn(List.of(metrica));

            List<Map<String, Object>> resultado = kpiBffService.getKpisForDashboard();

            @SuppressWarnings("unchecked")
            List<Map<String, Object>> metricas =
                    (List<Map<String, Object>>) resultado.get(0).get("metricas");

            assertThat(metricas.get(0).get("sucursalNombre")).isEqualTo("Dato Global");
            verify(sucursalClient, never()).obtenerPorId(any());
        }

        @Test
        @DisplayName("Lista de definiciones vacía: retorna lista vacía sin errores")
        void sinDefiniciones_retornaListaVacia() {
            when(kpiClient.obtenerTodosLosKpis()).thenReturn(List.of());

            List<Map<String, Object>> resultado = kpiBffService.getKpisForDashboard();

            assertThat(resultado).isEmpty();
            verify(ventasClient, never()).listarVentas(any(), any(), any());
        }

        @Test
        @DisplayName("VentasClient retorna null: valorActual queda en 0.0")
        void ventasClientRetornaNull_valorActualEsCero() {
            Map<String, Object> defVentas = buildDefinicion(1L, "ventas anuales");

            when(kpiClient.obtenerTodosLosKpis()).thenReturn(List.of(defVentas));
            when(ventasClient.listarVentas(null, null, null)).thenReturn(null);

            List<Map<String, Object>> resultado = kpiBffService.getKpisForDashboard();

            @SuppressWarnings("unchecked")
            List<Map<String, Object>> metricas =
                    (List<Map<String, Object>>) resultado.get(0).get("metricas");

            assertThat(metricas.get(0).get("valorActual")).isEqualTo(0.0);
        }
    }

    // ─── crearDefinicion / crearMetrica ──────────────────────────────────────

    @Nested
    @DisplayName("crearDefinicion y crearMetrica")
    class CrearDelegaciones {

        @Test
        @DisplayName("crearDefinicion delega directamente al KpiClient")
        void crearDefinicion_delegaAKpiClient() {
            Map<String, Object> payload = Map.of("nombre", "Nuevas Ventas");
            Map<String, Object> respuesta = Map.of("id", 10, "nombre", "Nuevas Ventas");

            when(kpiClient.crearDefinicion(payload)).thenReturn(respuesta);

            Map<String, Object> resultado = kpiBffService.crearDefinicion(payload);

            assertThat(resultado.get("id")).isEqualTo(10);
            verify(kpiClient, times(1)).crearDefinicion(payload);
        }

        @Test
        @DisplayName("crearMetrica delega directamente al KpiClient")
        void crearMetrica_delegaAKpiClient() {
            Map<String, Object> payload = Map.of("definicionId", 1, "valorActual", 5000.0);
            Map<String, Object> respuesta = Map.of("id", 20, "valorActual", 5000.0);

            when(kpiClient.crearMetrica(payload)).thenReturn(respuesta);

            Map<String, Object> resultado = kpiBffService.crearMetrica(payload);

            assertThat(resultado.get("id")).isEqualTo(20);
            verify(kpiClient, times(1)).crearMetrica(payload);
        }
    }
}