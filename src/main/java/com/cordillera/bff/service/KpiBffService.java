package com.cordillera.bff.service;

import com.cordillera.bff.client.KpiClient;
import com.cordillera.bff.client.SucursalClient;
import com.cordillera.bff.client.VentasClient;
import com.cordillera.bff.dto.SucursalResponseDto;
import com.cordillera.bff.dto.VentaResponseDto;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class KpiBffService {

    @Autowired
    private KpiClient kpiClient;

    @Autowired
    private SucursalClient sucursalClient;

    @Autowired
    private VentasClient ventasClient;

    public List<Map<String, Object>> getKpisForDashboard() {
        // 1. Obtener todas las definiciones de KPIs desde el ms-kpi
        List<Map<String, Object>> definiciones = kpiClient.obtenerTodosLosKpis();

        for (Map<String, Object> definicion : definiciones) {
            String nombreKpi = (String) definicion.get("nombre");

            // Log para debuggear el nombre en consola
            System.out.println("DEBUG: Procesando KPI: " + nombreKpi);

            // 2. Lógica para KPI de Ventas (Automatizado)
            if (nombreKpi != null && nombreKpi.toLowerCase().contains("ventas")) {
                try {
                    List<VentaResponseDto> ventas = ventasClient.listarVentas();

                    double sumaTotal = 0.0;
                    if (ventas != null) {
                        sumaTotal = ventas.stream()
                                .filter(v -> v != null && v.getMontoTotal() != null)
                                .mapToDouble(v -> v.getMontoTotal().doubleValue())
                                .sum();
                    }

                    System.out.println("DEBUG: Suma calculada de ventas: " + sumaTotal);

                    // Creamos una métrica dinámica limpia
                    Map<String, Object> metricaDinamica = new HashMap<>();
                    metricaDinamica.put("valorActual", sumaTotal);
                    metricaDinamica.put("sucursalNombre", "Global Consolidado");

                    // FORZAMOS la limpieza y reemplazo total de métricas
                    List<Map<String, Object>> listaMetricas = new ArrayList<>();
                    listaMetricas.add(metricaDinamica);

                    definicion.put("metricas", listaMetricas);

                    // Saltamos al siguiente KPI para no procesar lógica manual
                    continue;

                } catch (Exception e) {
                    System.err.println("❌ ERROR CRÍTICO EN CÁLCULO DE VENTAS: " + e.getMessage());
                    e.printStackTrace();
                }
            }

            // 3. Flujo normal para KPIs manuales
            Long definicionId = ((Number) definicion.get("id")).longValue();
            List<Map<String, Object>> metricas = kpiClient.obtenerMetricasPorKpi(definicionId);

            for (Map<String, Object> metrica : metricas) {
                if (metrica.get("sucursalId") != null) {
                    Long sucursalId = ((Number) metrica.get("sucursalId")).longValue();
                    try {
                        SucursalResponseDto sucursal = sucursalClient.obtenerPorId(sucursalId);
                        metrica.put("sucursalNombre", sucursal.getNombre());
                    } catch (Exception e) {
                        metrica.put("sucursalNombre", "Sucursal Desconocida");
                    }
                } else {
                    metrica.put("sucursalNombre", "Dato Global");
                }
            }
            definicion.put("metricas", metricas);
        }

        return definiciones;
    }
}