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
import java.util.stream.Collectors;

@Service
public class CatalogoBffService {

    @Autowired
    private ProductoClient productoClient;

    @Autowired
    private CategoriaClient categoriaClient;

    @Autowired
    private StockClient stockClient;

    // EL MÉTODO QUE YA TENÍAS
    public CatalogoDashboardDTO obtenerVistaCatalogo(Long productoId) {
        ProductoResponseDTO producto = productoClient.obtenerProductoPorId(productoId);

        String nombreCat = "Sin Categoría";
        if (producto.getCategoriaId() != null) {
            try {
                nombreCat = categoriaClient.obtenerCategoriaPorId(producto.getCategoriaId()).getNombre();
            } catch (Exception e) {
                nombreCat = "Categoría no disponible";
            }
        }

        List<StockResponseDTO> stocks = stockClient.obtenerStockPorProducto(productoId);
        int stockTotal = stocks.stream()
                .mapToInt(StockResponseDTO::getCantidadDisponible)
                .sum();

        return CatalogoDashboardDTO.builder()
                .sku(producto.getSku())
                .nombreProducto(producto.getNombre())
                .precio(producto.getPrecio())
                .nombreCategoria(nombreCat)
                .stockTotalDisponible(stockTotal)
                .build();
    }

    // --- NUEVO MÉTODO PARA EL LISTADO COMPLETO ---
    public List<CatalogoDashboardDTO> listarCatalogoCompleto() {
        // 1. Obtenemos la lista completa de productos desde el microservicio de productos
        // Nota: Asegúrate de tener este método definido en tu ProductoClient
        List<ProductoResponseDTO> productos = productoClient.obtenerTodosLosProductos();

        // 2. Transformamos cada ProductoResponseDTO en un CatalogoDashboardDTO
        return productos.stream().map(producto -> {

            // A. Buscamos la categoría de este producto específico
            String nombreCat = "Sin Categoría";
            if (producto.getCategoriaId() != null) {
                try {
                    nombreCat = categoriaClient.obtenerCategoriaPorId(producto.getCategoriaId()).getNombre();
                } catch (Exception e) {
                    nombreCat = "Categoría no disponible";
                }
            }

            // B. Buscamos y sumamos el stock de este producto en las distintas sucursales
            int stockTotal = 0;
            try {
                List<StockResponseDTO> stocks = stockClient.obtenerStockPorProducto(producto.getId()); // Asumiendo que producto tiene getId()
                stockTotal = stocks.stream()
                        .mapToInt(StockResponseDTO::getCantidadDisponible)
                        .sum();
            } catch (Exception e) {
                // Si el microservicio de stock cae, devolvemos 0 en lugar de romper toda la tabla
            }

            // C. Ensamblamos el DTO para el frontend
            return CatalogoDashboardDTO.builder()
                    // Asegúrate de mapear los campos extra que pusimos en React si los agregas a tu DTO en Java
                    // (ej. id, descripcion, costo, activo)
                    .sku(producto.getSku())
                    .nombreProducto(producto.getNombre())
                    .precio(producto.getPrecio())
                    .nombreCategoria(nombreCat)
                    .stockTotalDisponible(stockTotal)
                    .build();

        }).collect(Collectors.toList());
    }
}