package com.cordillera.bff.service;

import com.cordillera.bff.client.CategoriaClient;
import com.cordillera.bff.client.ProductoClient;
import com.cordillera.bff.client.StockClient;
import com.cordillera.bff.dto.CatalogoDashboardDTO;
import com.cordillera.bff.dto.ProductoResponseDTO;
import com.cordillera.bff.dto.StockResponseDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CatalogoBffService {

    @Autowired
    private ProductoClient productoClient;

    @Autowired
    private CategoriaClient categoriaClient;

    @Autowired
    private StockClient stockClient;

    public CatalogoDashboardDTO obtenerVistaCatalogo(Long productoId) {
        // 1. Buscamos el Producto
        ProductoResponseDTO producto = productoClient.obtenerProductoPorId(productoId);

        // 2. Con el categoriaId del producto, buscamos el nombre de la Categoría
        String nombreCat = "Sin Categoría";
        if (producto.getCategoriaId() != null) {
            try {
                nombreCat = categoriaClient.obtenerCategoriaPorId(producto.getCategoriaId()).getNombre();
            } catch (Exception e) {
                nombreCat = "Categoría no disponible"; // Fallback simple
            }
        }

        // 3. Buscamos todo el stock de ese producto en todas las sucursales
        List<StockResponseDTO> stocks = stockClient.obtenerStockPorProducto(productoId);

        // 4. Sumamos el stock de todas las sucursales para mostrar un "Stock Total"
        int stockTotal = stocks.stream()
                .mapToInt(StockResponseDTO::getCantidadDisponible)
                .sum();

        // 5. Ensamblamos el DTO final a la medida del Frontend
        return CatalogoDashboardDTO.builder()
                .sku(producto.getSku())
                .nombreProducto(producto.getNombre())
                .precio(producto.getPrecio())
                .nombreCategoria(nombreCat)
                .stockTotalDisponible(stockTotal)
                .build();
    }
}
