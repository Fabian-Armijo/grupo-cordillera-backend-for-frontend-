package com.cordillera.bff.service;

import com.cordillera.bff.client.ProductoClient;
import com.cordillera.bff.client.StockClient;
import com.cordillera.bff.dto.CatalogoDashboardDTO;
import com.cordillera.bff.dto.ProductoResponseDTO;
import com.cordillera.bff.dto.StockResponseDTO;
import com.cordillera.bff.dto.RespuestaResilienteDto;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Service
public class CatalogoBffService {

    @Autowired
    private ProductoClient productoClient;

    @Autowired
    private StockClient stockClient;

    // 🛡️ CACHÉ "MODO DIOS": Bóveda en memoria nativa
    private final Map<String, List<CatalogoDashboardDTO>> memoriaCatalogo = new ConcurrentHashMap<>();
    private final Map<String, String> memoriaHora = new ConcurrentHashMap<>();

    public RespuestaResilienteDto<List<CatalogoDashboardDTO>> listarCatalogoCompleto(Long sucursalIdHeader) {

        Long sucursalIdUsuario = null;

        // --- 1. RESOLUCIÓN DE SUCURSAL ---
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth != null) {
                if (auth.getPrincipal() instanceof Map) {
                    Map<?, ?> principalMap = (Map<?, ?>) auth.getPrincipal();
                    if (principalMap.containsKey("sucursalId") && principalMap.get("sucursalId") != null) {
                        sucursalIdUsuario = Long.valueOf(principalMap.get("sucursalId").toString());
                    }
                }
                if (sucursalIdUsuario == null && auth.getDetails() instanceof Map) {
                    Map<?, ?> details = (Map<?, ?>) auth.getDetails();
                    if (details.containsKey("sucursalId") && details.get("sucursalId") != null) {
                        sucursalIdUsuario = Long.valueOf(details.get("sucursalId").toString());
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("⚠️ Error leyendo SecurityContext: " + e.getMessage());
        }

        if (sucursalIdUsuario == null) sucursalIdUsuario = sucursalIdHeader;
        if (sucursalIdUsuario == null) sucursalIdUsuario = 7L;

        String llaveSucursal = "sucursal_" + sucursalIdUsuario;

        // --- 2. EJECUCIÓN PROTEGIDA ---
        try {
            // A. Consultamos el Stock
            List<StockResponseDTO> stockSucursal = stockClient.obtenerStockPorSucursal(sucursalIdUsuario);

            if (stockSucursal == null || stockSucursal.isEmpty()) {
                return new RespuestaResilienteDto<>(List.of());
            }

            // B. Consultamos los Productos
            List<ProductoResponseDTO> todosLosProductos = productoClient.obtenerTodosLosProductos();

            // C. Mapeamos y Unificamos (Filtrando silenciosamente los fantasmas sin lanzar alarmas)
            Map<Long, ProductoResponseDTO> mapaProductos = todosLosProductos != null
                    ? todosLosProductos.stream()
                    .filter(p -> p.getId() != null)
                    .collect(Collectors.toMap(ProductoResponseDTO::getId, p -> p, (p1, p2) -> p1))
                    : Map.of();

            List<CatalogoDashboardDTO> resultado = stockSucursal.stream()
                    .filter(stock -> stock.getCantidadDisponible() != null && stock.getCantidadDisponible() > 0)
                    .map(stock -> {
                        ProductoResponseDTO producto = mapaProductos.get(stock.getProductoId());

                        // Si el producto no existe en ms-productos (Ej: El famoso ID 13), lo ignoramos silenciosamente
                        if (producto == null) return null;

                        return CatalogoDashboardDTO.builder()
                                .id(stock.getProductoId())
                                .sku(producto.getSku())
                                .nombreProducto(producto.getNombre())
                                .precio(producto.getPrecio())
                                .nombreCategoria("General")
                                .stockTotalDisponible(stock.getCantidadDisponible())
                                .build();
                    })
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());

            // 🌟 ÉXITO TOTAL: Guardamos la lista validada en nuestra bóveda privada
            memoriaCatalogo.put(llaveSucursal, resultado);
            memoriaHora.put(llaveSucursal, java.time.LocalTime.now().format(java.time.format.DateTimeFormatter.ofPattern("HH:mm")));

            return new RespuestaResilienteDto<>(resultado);

        } catch (Exception e) {
            // 🚨 FALLBACK REAL: Solo entramos aquí si Feign lanza error porque ms-productos o ms-stock explotaron o se apagaron
            System.err.println("🚨 [ALERTA BFF] Falla de red detectada. Rescatando caché de bóveda para: " + llaveSucursal + ". Causa: " + e.getMessage());

            if (memoriaCatalogo.containsKey(llaveSucursal)) {
                List<CatalogoDashboardDTO> datosRescatados = memoriaCatalogo.get(llaveSucursal);
                String horaRescate = memoriaHora.get(llaveSucursal);
                return new RespuestaResilienteDto<>(datosRescatados, horaRescate);
            }

            return new RespuestaResilienteDto<>(List.of());
        }
    }
}