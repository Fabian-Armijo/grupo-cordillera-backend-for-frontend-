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
import java.util.concurrent.ConcurrentHashMap; // 👈 La bóveda indestructible

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

        // 🌟 OPTIMIZACIÓN: Variables para calcular ventas UNA SOLA VEZ
        double sumaTotalVentasGlobal = 0.0;
        boolean ventasYaCalculadas = false;

        for (Map<String, Object> definicion : definiciones) {
            String nombreKpi = (String) definicion.get("nombre");

            // Verificamos si el nombre contiene "ventas"
            if (nombreKpi != null && nombreKpi.toLowerCase().contains("ventas")) {

                // Si es el primer KPI de ventas que encontramos, vamos al ms-ventas
                if (!ventasYaCalculadas) {
                    try {
                        List<VentaResponseDto> ventas = ventasClient.listarVentas();
                        if (ventas != null) {
                            sumaTotalVentasGlobal = ventas.stream()
                                    .filter(v -> v != null && v.getMontoTotal() != null)
                                    .mapToDouble(v -> v.getMontoTotal().doubleValue())
                                    .sum();
                        }
                        // Marcamos como true para que los siguientes KPIs no vuelvan a hacer la petición HTTP
                        ventasYaCalculadas = true;
                        System.out.println("DEBUG: Ventas consultadas exitosamente. Total global: " + sumaTotalVentasGlobal);
                    } catch (Exception e) {
                        System.err.println("❌ ERROR AL CONECTAR CON VENTAS: " + e.getMessage());
                    }
                }

                // Le inyectamos el total calculado a ESTE KPI específico
                Map<String, Object> metricaDinamica = new HashMap<>();
                metricaDinamica.put("valorActual", sumaTotalVentasGlobal);
                metricaDinamica.put("sucursalNombre", "Global Consolidado");

                List<Map<String, Object>> listaMetricas = new ArrayList<>();
                listaMetricas.add(metricaDinamica);

                definicion.put("metricas", listaMetricas);

                continue; // Pasamos a la siguiente definición
            }

            // --- Flujo normal para otros KPIs (Manuales) ---
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
    public Map<String, Object> crearDefinicion(Map<String, Object> definicion) {
        return kpiClient.crearDefinicion(definicion);
    }

    public Map<String, Object> crearMetrica(Map<String, Object> metrica) {
        return kpiClient.crearMetrica(metrica);
    }

    public Map<String, Object> crearDefinicion(Map<String, Object> definicion) { return kpiClient.crearDefinicion(definicion); }
    public Map<String, Object> crearMetrica(Map<String, Object> metrica) { return kpiClient.crearMetrica(metrica); }
}