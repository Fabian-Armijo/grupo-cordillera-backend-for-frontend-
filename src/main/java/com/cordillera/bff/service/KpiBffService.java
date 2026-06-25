package com.cordillera.bff.service;

import com.cordillera.bff.client.KpiClient;
import com.cordillera.bff.client.SucursalClient;
import com.cordillera.bff.client.VentasClient;
import com.cordillera.bff.dto.RespuestaResilienteDto; // 👈 Importamos nuestro sobre
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

    // 🛡️ CACHÉ "MODO DIOS": Bóveda en memoria nativa
    private final Map<String, List<Map<String, Object>>> memoriaKpis = new ConcurrentHashMap<>();
    private final Map<String, String> memoriaHora = new ConcurrentHashMap<>();

    // 🎯 Ahora devolvemos el Sobre (RespuestaResilienteDto)
    public RespuestaResilienteDto<List<Map<String, Object>>> getKpisForDashboard() {
        String llaveCache = "dashboard_global";

        try {
            // A. Consultamos la definición base de KPIs
            List<Map<String, Object>> definiciones = kpiClient.obtenerTodosLosKpis();

            // 🕵️‍♂️ DETECTOR DE FALLAS: Si ms-kpis está caído y devuelve nulo
            if (definiciones == null) {
                throw new RuntimeException("ms-kpis está caído o inaccesible. Forzando rescate.");
            }

            double sumaTotalVentasGlobal = 0.0;
            boolean ventasYaCalculadas = false;

            for (Map<String, Object> definicion : definiciones) {
                String nombreKpi = (String) definicion.get("nombre");

                if (nombreKpi != null && nombreKpi.toLowerCase().contains("ventas")) {
                    if (!ventasYaCalculadas) {
                        try {
                            List<VentaResponseDto> ventas = ventasClient.listarVentas(null, null, null);
                            if (ventas != null) {
                                sumaTotalVentasGlobal = ventas.stream()
                                        .filter(v -> v != null && v.getMontoTotal() != null)
                                        .mapToDouble(v -> v.getMontoTotal().doubleValue())
                                        .sum();
                            }
                            ventasYaCalculadas = true;
                        } catch (Exception e) {
                            System.err.println("⚠️ Advertencia: ms-ventas inaccesible desde KPI. Las ventas dinámicas mostrarán 0 por ahora.");
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

                // Usamos un pequeño try-catch por si falla obtener el detalle de un KPI específico
                List<Map<String, Object>> metricas;
                try {
                    metricas = kpiClient.obtenerMetricasPorKpi(definicionId);
                    if (metricas == null) metricas = new ArrayList<>();
                } catch (Exception e) {
                    metricas = new ArrayList<>();
                }

                for (Map<String, Object> metrica : metricas) {
                    if (metrica.get("sucursalId") != null) {
                        Long sucursalId = ((Number) metrica.get("sucursalId")).longValue();
                        try {
                            SucursalResponseDto sucursal = sucursalClient.obtenerPorId(sucursalId);
                            metrica.put("sucursalNombre", sucursal != null ? sucursal.getNombre() : "Sucursal Desconocida");
                        } catch (Exception e) {
                            metrica.put("sucursalNombre", "Sucursal Desconocida");
                        }
                    } else {
                        metrica.put("sucursalNombre", "Dato Global");
                    }
                }
                definicion.put("metricas", metricas);
            }

            // 🌟 ÉXITO TOTAL: Guardamos la foto final del Dashboard en la bóveda
            memoriaKpis.put(llaveCache, definiciones);
            memoriaHora.put(llaveCache, java.time.LocalTime.now().format(java.time.format.DateTimeFormatter.ofPattern("HH:mm")));

            return new RespuestaResilienteDto<>(definiciones);

        } catch (Exception e) {
            // 🚨 FALLBACK: Si ms-kpis explota o la red falla, vamos a la bóveda
            System.err.println("🚨 [ALERTA BFF] Error orquestando Dashboard de KPIs. Rescatando caché... Motivo: " + e.getMessage());

            if (memoriaKpis.containsKey(llaveCache)) {
                List<Map<String, Object>> datosRescatados = memoriaKpis.get(llaveCache);
                String horaRescate = memoriaHora.get(llaveCache);

                System.out.println("✅ [BFF] ¡Rescate exitoso! Entregando Dashboard de las " + horaRescate);
                return new RespuestaResilienteDto<>(datosRescatados, horaRescate);
            }

            System.err.println("❌ [BFF] La bóveda está vacía. Entregando Dashboard vacío.");
            return new RespuestaResilienteDto<>(new ArrayList<>());
        }
    }

    public Map<String, Object> crearDefinicion(Map<String, Object> definicion) { return kpiClient.crearDefinicion(definicion); }
    public Map<String, Object> crearMetrica(Map<String, Object> metrica) { return kpiClient.crearMetrica(metrica); }
}