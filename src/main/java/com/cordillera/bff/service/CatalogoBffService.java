package com.cordillera.bff.service;

import com.cordillera.bff.client.ProductoClient;
import com.cordillera.bff.client.StockClient;
import com.cordillera.bff.dto.CatalogoDashboardDTO;
import com.cordillera.bff.dto.ProductoResponseDTO;
import com.cordillera.bff.dto.StockResponseDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class CatalogoBffService {

    @Autowired
    private ProductoClient productoClient;

    @Autowired
    private StockClient stockClient;

    // Inyectamos el JwtService para poder leer el token directamente
    @Autowired
    private JwtService jwtService;

    public List<CatalogoDashboardDTO> listarCatalogoCompleto(Long sucursalIdHeader) {
        Long sucursalIdUsuario = null;

        // 🏢 PASO 1: Recuperar la sucursal directamente desde el Token JWT guardado en Seguridad
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            // En nuestro JwtAuthenticationFilter, guardamos el token como 'Credentials'
            if (auth != null && auth.getCredentials() != null) {
                String token = auth.getCredentials().toString();
                sucursalIdUsuario = jwtService.extractSucursalId(token);

                if (sucursalIdUsuario != null) {
                    System.out.println("✅ [GATEWAY-SERVICE] Sucursal rescatada exitosamente desde el JWT de seguridad: " + sucursalIdUsuario);
                }
            }
        } catch (Exception e) {
            System.err.println("⚠️ No se pudo extraer la sucursal del token: " + e.getMessage());
        }

        // 🏢 PASO 2: Si la autenticación falló, intentamos usar la cabecera (Fallback)
        if (sucursalIdUsuario == null) {
            sucursalIdUsuario = sucursalIdHeader;
        }

        // 🏢 PASO 3: ZERO TRUST (Adiós Sucursal 7)
        // Si nadie nos dice qué sucursal es, bloqueamos la operación por seguridad.
        if (sucursalIdUsuario == null) {
            System.err.println("❌ [GATEWAY-SERVICE] Error Crítico: No se pudo determinar la sucursal del usuario. Operación denegada.");
            throw new RuntimeException("Acceso denegado: No se pudo verificar a qué sucursal pertenece el usuario.");
        }

        System.out.println("🔄 [GATEWAY-SERVICE] -> Solicitando inventario a ms-stock para la sucursal REAL: " + sucursalIdUsuario);

        // 2. Traer todo el stock de esa sucursal
        List<StockResponseDTO> stockSucursal;
        try {
            stockSucursal = stockClient.obtenerStockPorSucursal(sucursalIdUsuario);
        } catch (Exception e) {
            System.err.println("❌ Error crítico llamando a ms-stock por sucursal: " + e.getMessage());
            return List.of();
        }

        if (stockSucursal == null || stockSucursal.isEmpty()) {
            System.out.println("⚠️ [GATEWAY-SERVICE] No se encontraron registros de inventario en la sucursal: " + sucursalIdUsuario);
            return List.of();
        }

        // 3. Traer todos los productos de ms-productos
        System.out.println("📦 [GATEWAY-SERVICE] -> Cruzando datos asíncronos con ms-productos...");
        List<ProductoResponseDTO> todosLosProductos = productoClient.obtenerTodosLosProductos();
        if (todosLosProductos == null || todosLosProductos.isEmpty()) {
            return List.of();
        }

        // Convertimos los productos en un Mapa en memoria
        Map<Long, ProductoResponseDTO> mapaProductos = todosLosProductos.stream()
                .collect(Collectors.toMap(ProductoResponseDTO::getId, p -> p, (p1, p2) -> p1));

        // 4. Procesamos la lista final
        return stockSucursal.stream()
                .filter(stock -> stock.getCantidadDisponible() != null && stock.getCantidadDisponible() > 0)
                .map(stock -> {
                    ProductoResponseDTO prod = mapaProductos.get(stock.getProductoId());

                    String nombre = (prod != null) ? prod.getNombre() : "Producto Descatalogado (ID: " + stock.getProductoId() + ")";
                    String sku = (prod != null) ? prod.getSku() : "S/N";
                    Double precio = (prod != null) ? prod.getPrecio() : 0.0;

                    return CatalogoDashboardDTO.builder()
                            .id(stock.getProductoId())
                            .sku(sku)
                            .nombreProducto(nombre)
                            .precio(precio)
                            .nombreCategoria("General")
                            .stockTotalDisponible(stock.getCantidadDisponible())
                            .build();
                })
                .collect(Collectors.toList());
    }
}