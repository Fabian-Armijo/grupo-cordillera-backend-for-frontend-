package com.cordillera.bff.service;

import com.cordillera.bff.client.VentasClient;
import com.cordillera.bff.client.SucursalClient;
import com.cordillera.bff.client.KpiClient;
import com.cordillera.bff.dto.VentaRequestDto;
import com.cordillera.bff.dto.VentaResponseDto;
import com.cordillera.bff.dto.SucursalResponseDto;
import com.cordillera.bff.dto.RespuestaResilienteDto; // 👈 ¡Importación del nuevo "Sobre"!

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class VentasBffService {

    @Autowired
    private VentasClient ventaClient; // Cliente original para ms-ventas

    @Autowired
    private SucursalClient sucursalClient;

    @Autowired
    private KpiClient kpiClient; // Conexión al puerto 8087 para automatizar gráficos y reportes

    @Autowired
    private CacheManager cacheManager; // Nuestro gestor de salvavidas en memoria

    // 🛡️ PATRÓN ENVELOPE + TRY/CATCH: La forma más segura de enviar caché y alertas al Frontend
    public RespuestaResilienteDto<List<VentaResponseDto>> listarTodasLasVentas(String userRole, Long sucursalId, String token) {

        // Llaves para guardar la información y la hora exacta
        String cacheKey = "ventas_" + (sucursalId != null ? sucursalId : "GLOBAL");
        String timeKey = "ventas_hora_" + (sucursalId != null ? sucursalId : "GLOBAL");

        Cache cache = cacheManager.getCache("ventasCache");

        try {
            // 1. Intentamos obtener la información viva y real
            List<VentaResponseDto> ventasVivas = ventaClient.listarVentas(userRole, sucursalId, token);

            // 2. Si funciona, actualizamos el salvavidas y la HORA EXACTA
            if (cache != null) {
                cache.put(cacheKey, ventasVivas);
                // Guardamos la hora actual en formato HH:mm (Ej: 14:30)
                cache.put(timeKey, java.time.LocalTime.now().format(java.time.format.DateTimeFormatter.ofPattern("HH:mm")));
            }

            // Retornamos el sobre indicando que los datos son FRESCOS (fromCache = false)
            return new RespuestaResilienteDto<>(ventasVivas);

        } catch (Exception e) {
            // 🚨 3. SI EXPLOTA (ms-ventas caído), rescatamos datos y hora de la RAM
            System.err.println("🚨 [ALERTA BFF] ms-ventas está CAÍDO. Rescatando caché para: " + cacheKey);

            if (cache != null && cache.get(cacheKey) != null) {
                @SuppressWarnings("unchecked")
                List<VentaResponseDto> datosCacheados = (List<VentaResponseDto>) cache.get(cacheKey).get();
                String horaGuardada = (String) cache.get(timeKey).get();

                System.out.println("✅ [BFF] Entregando datos de respaldo a React. Hora de caché: " + horaGuardada);
                // Retornamos el sobre indicando que es CACHÉ y enviamos la hora
                return new RespuestaResilienteDto<>(datosCacheados, horaGuardada);
            }

            // Si nunca hubo caché, enviamos el sobre con una lista vacía
            System.err.println("❌ [BFF] No hay caché disponible. Retornando tabla vacía.");
            return new RespuestaResilienteDto<>(new ArrayList<>());
        }
    }

    // 🎯 ORQUESTACIÓN TRANSACCIONAL CORREGIDA: Usa el acumulador nativo y amarra la sucursal real
    public VentaResponseDto procesarVenta(VentaRequestDto request) {
        System.out.println("🛒 [BFF-VENTAS] -> Paso 1: Solicitando autorización de transacción a ms-ventas...");

        // FASE 1: Se crea y confirma la venta en ms-ventas
        VentaResponseDto ventaConfirmada = ventaClient.crearVenta(request);

        // Determinamos con seguridad la sucursal real (priorizando la base de datos o el request)
        Long sucursalReal = ventaConfirmada.getSucursalId() != null ? ventaConfirmada.getSucursalId() : request.getSucursalId();

        System.out.println("✅ [BFF-VENTAS] -> Venta confirmada de forma exitosa para Sucursal ID: " + sucursalReal + ". ID: " + ventaConfirmada.getId());

        // FASE 2: Orquestación en caliente hacia el método acumulador de ms-kpi (Puerto 8087)
        try {
            System.out.println("📊 [BFF-VENTAS] -> Paso 2: Despachando ítems al acumulador de ms-kpi...");

            // Estructuramos el mapa del producto vendido tal como lo procesa internamente el KpiService
            Map<String, Object> itemVendido = new HashMap<>();
            itemVendido.put("productoId", ventaConfirmada.getProductoId() != null ? ventaConfirmada.getProductoId() : request.getProductoId());
            itemVendido.put("cantidad", ventaConfirmada.getCantidad() != null ? ventaConfirmada.getCantidad() : request.getCantidad());
            itemVendido.put("montoTotal", ventaConfirmada.getMontoTotal() != null ? ventaConfirmada.getMontoTotal() : request.getMontoTotal());

            // El endpoint del microservicio espera una lista de productos
            List<Map<String, Object>> listaProductos = new ArrayList<>();
            listaProductos.add(itemVendido);

            // 🚀 Mandamos la sucursal explícitamente para que impacte el KPI que corresponde
            kpiClient.acumularProgreso(sucursalReal, listaProductos);
            System.out.println("⭐ [BFF-VENTAS] -> ms-kpi recalculó exitosamente las métricas para la sucursal " + sucursalReal);

        } catch (Exception e) {
            // Aislamos el catch para que si el microservicio de KPIs se cae, no interrumpa la venta en el Front
            System.err.println("⚠️ [BFF-VENTAS] No se pudo acumular el KPI en tiempo real. Causa: " + e.getMessage());
        }

        return ventaConfirmada;
    }

    public List<SucursalResponseDto> obtenerSucursalesParaVenta() {
        return sucursalClient.listarTodas();
    }
}