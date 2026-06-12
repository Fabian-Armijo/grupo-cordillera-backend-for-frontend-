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
        List<Map<String, Object>> definiciones = kpiClient.obtenerTodosLosKpis();

        double sumaTotalVentasGlobal = 0.0;
        boolean ventasYaCalculadas = false;

        for (Map<String, Object> definicion : definiciones) {
            String nombreKpi = (String) definicion.get("nombre");

            if (nombreKpi != null && nombreKpi.toLowerCase().contains("ventas")) {
                if (!ventasYaCalculadas) {
                    try {
                        // 🚀 CORREGIDO PARA AJUSTARSE A LA INTERFAZ:
                        // Se pasan nulls explícitos ya que el Dashboard no requiere filtrar estas ventas por cabeceras manuales.
                        List<VentaResponseDto> ventas = ventasClient.listarVentas(null, null, null);
                        if (ventas != null) {
                            sumaTotalVentasGlobal = ventas.stream()
                                    .filter(v -> v != null && v.getMontoTotal() != null)
                                    .mapToDouble(v -> v.getMontoTotal().doubleValue())
                                    .sum();
                        }
                        ventasYaCalculadas = true;
                    } catch (Exception e) {
                        System.err.println("❌ ERROR AL CONECTAR CON VENTAS DESDE KPI: " + e.getMessage());
                    }
                }

                Map<String, Object> metricaDinamica = new HashMap<>();
                metricaDinamica.put("valorActual", sumaTotalVentasGlobal);
                metricaDinamica.put("sucursalNombre", "Global Consolidado");

                List<Map<String, Object>> listaMetricas = new ArrayList<>();
                listaMetricas.add(metricaDinamica);
                definicion.put("metricas", listaMetricas);
                continue;
            }

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

    public Map<String, Object> crearDefinicion(Map<String, Object> definicion) { return kpiClient.crearDefinicion(definicion); }
    public Map<String, Object> crearMetrica(Map<String, Object> metrica) { return kpiClient.crearMetrica(metrica); }
}