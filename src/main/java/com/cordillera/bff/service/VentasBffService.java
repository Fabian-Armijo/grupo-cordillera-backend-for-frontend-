package com.cordillera.bff.service;

import com.cordillera.bff.client.VentasClient; // 🎯 Tu cliente original para ms-ventas
import com.cordillera.bff.client.SucursalClient;
import com.cordillera.bff.client.KpiClient;      // 🎯 Tu cliente para alimentar ms-kpi
import com.cordillera.bff.dto.VentaRequestDto;
import com.cordillera.bff.dto.VentaResponseDto;
import com.cordillera.bff.dto.SucursalResponseDto;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class VentasBffService {

    @Autowired
    private VentasClient ventaClient; // 👈 Tu variable declarada en singular

    @Autowired
    private SucursalClient sucursalClient;

    @Autowired
    private KpiClient kpiClient; // 🎯 Conexión al puerto 8087 para automatizar gráficos y reportes

    // 🚀 Recibe los parámetros del controlador y se los pasa a ventaClient
    public List<VentaResponseDto> listarTodasLasVentas(String userRole, Long sucursalId, String token) {
        return ventaClient.listarVentas(userRole, sucursalId, token);
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

            // 🚀 SOLUCIONADO: Mandamos la sucursal explícitamente para que impacte el KPI que corresponde
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