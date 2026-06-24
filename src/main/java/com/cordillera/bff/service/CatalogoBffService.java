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

    // 🎯 EXTRACCIÓN MEJORADA: Obtiene la sucursal de forma directa y robusta
    public List<CatalogoDashboardDTO> listarCatalogoCompleto(Long sucursalIdHeader) {

        Long sucursalIdUsuario = null;

        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            System.out.println("======================================");
            System.out.println("AUTH: " + auth);
            System.out.println("PRINCIPAL: " + auth.getPrincipal());
            System.out.println("DETAILS: " + auth.getDetails());
            System.out.println("AUTHORITIES: " + auth.getAuthorities());
            System.out.println("======================================");
            if (auth != null) {

                if (auth.getPrincipal() instanceof Map) {
                    Map<?, ?> principalMap = (Map<?, ?>) auth.getPrincipal();

                    if (principalMap.containsKey("sucursalId")
                            && principalMap.get("sucursalId") != null) {

                        sucursalIdUsuario =
                                Long.valueOf(principalMap.get("sucursalId").toString());
                    }
                }

                if (sucursalIdUsuario == null
                        && auth.getDetails() instanceof Map) {

                    Map<?, ?> details =
                            (Map<?, ?>) auth.getDetails();

                    if (details.containsKey("sucursalId")
                            && details.get("sucursalId") != null) {

                        sucursalIdUsuario =
                                Long.valueOf(details.get("sucursalId").toString());
                    }
                }
            }

        } catch (Exception e) {
            System.err.println(
                    "⚠️ Error leyendo SecurityContext: "
                            + e.getMessage()
            );
        }

        if (sucursalIdUsuario == null) {
            sucursalIdUsuario = sucursalIdHeader;
        }

        if (sucursalIdUsuario == null) {
            System.out.println(
                    "⚠️ No se encontró sucursal en contexto ni header."
            );

            sucursalIdUsuario = 7L;
        }

        System.out.println("======================================");
        System.out.println("SUCURSAL DETECTADA: " + sucursalIdUsuario);
        System.out.println("======================================");

        List<StockResponseDTO> stockSucursal;

        try {

            stockSucursal =
                    stockClient.obtenerStockPorSucursal(sucursalIdUsuario);

        } catch (Exception e) {

            System.err.println(
                    "❌ Error consultando ms-stock: "
                            + e.getMessage()
            );

            return List.of();
        }

        System.out.println("======================================");
        System.out.println("STOCK RECIBIDO DESDE MS-STOCK");
        System.out.println("TOTAL STOCKS: "
                + (stockSucursal == null ? 0 : stockSucursal.size()));

        if (stockSucursal != null) {

            stockSucursal.forEach(stock ->
                    System.out.println(
                            "ProductoId="
                                    + stock.getProductoId()
                                    + " | Stock="
                                    + stock.getCantidadDisponible()
                    )
            );
        }

        System.out.println("======================================");

        if (stockSucursal == null || stockSucursal.isEmpty()) {

            System.out.println(
                    "⚠️ No existen registros de stock para sucursal "
                            + sucursalIdUsuario
            );

            return List.of();
        }

        List<ProductoResponseDTO> todosLosProductos;

        try {

            todosLosProductos =
                    productoClient.obtenerTodosLosProductos();

        } catch (Exception e) {

            System.err.println(
                    "❌ Error consultando ms-productos: "
                            + e.getMessage()
            );

            return List.of();
        }

        System.out.println("======================================");
        System.out.println("PRODUCTOS RECIBIDOS DESDE MS-PRODUCTOS");

        if (todosLosProductos == null) {

            System.out.println("LA LISTA VIENE NULL");

            return List.of();
        }

        System.out.println("TOTAL PRODUCTOS: "
                + todosLosProductos.size());

        todosLosProductos.forEach(p ->
                System.out.println(
                        "ID=" + p.getId()
                                + " | Nombre=" + p.getNombre()
                                + " | Sucursal=" + p.getSucursalId()
                )
        );

        System.out.println("======================================");

        Map<Long, ProductoResponseDTO> mapaProductos =
                todosLosProductos.stream()
                        .filter(p -> p.getId() != null)
                        .collect(
                                Collectors.toMap(
                                        ProductoResponseDTO::getId,
                                        p -> p,
                                        (p1, p2) -> p1
                                )
                        );

        List<CatalogoDashboardDTO> resultado = stockSucursal.stream()
                .filter(stock ->
                        stock.getCantidadDisponible() != null
                                && stock.getCantidadDisponible() > 0
                )
                .map(stock -> {

                    ProductoResponseDTO producto =
                            mapaProductos.get(stock.getProductoId());

                    if (producto == null) {

                        System.out.println(
                                "⚠️ PRODUCTO NO ENCONTRADO EN MS-PRODUCTOS -> ID="
                                        + stock.getProductoId()
                        );
                    }

                    return CatalogoDashboardDTO.builder()
                            .id(stock.getProductoId())
                            .sku(
                                    producto != null
                                            ? producto.getSku()
                                            : "S/N"
                            )
                            .nombreProducto(
                                    producto != null
                                            ? producto.getNombre()
                                            : "Producto Descatalogado (ID: "
                                            + stock.getProductoId() + ")"
                            )
                            .precio(
                                    producto != null
                                            ? producto.getPrecio()
                                            : 0.0
                            )
                            .nombreCategoria("General")
                            .stockTotalDisponible(
                                    stock.getCantidadDisponible()
                            )
                            .build();
                })
                .collect(Collectors.toList());

        System.out.println("======================================");
        System.out.println("CATALOGO FINAL ENVIADO A REACT");
        System.out.println("TOTAL ITEMS: " + resultado.size());

        resultado.forEach(item ->
                System.out.println(
                        item.getId()
                                + " | "
                                + item.getNombreProducto()
                                + " | stock="
                                + item.getStockTotalDisponible()
                )
        );

        System.out.println("======================================");

        return resultado;
    }
}