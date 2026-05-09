package com.cordillera.bff;


import com.cordillera.bff.client.CategoriaClient;
import com.cordillera.bff.client.ProductoClient;
import com.cordillera.bff.client.StockClient;
import com.cordillera.bff.dto.CatalogoDashboardDTO;
import com.cordillera.bff.dto.CategoriaResponseDTO;
import com.cordillera.bff.dto.ProductoResponseDTO;
import com.cordillera.bff.dto.StockResponseDTO;
import com.cordillera.bff.service.CatalogoBffService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CatalogoBffServiceTest {

    // 1. Mocks de los clientes Feign
    @Mock
    private ProductoClient productoClient;

    @Mock
    private CategoriaClient categoriaClient;

    @Mock
    private StockClient stockClient;

    // 2. Inyección en tu Service
    @InjectMocks
    private CatalogoBffService bffService;

    // ====================================================================
    // PRUEBA 1: El Camino Feliz (Los 3 microservicios responden bien)
    // ====================================================================
    @Test
    void obtenerVistaCatalogo_DeberiaUnirTodosLosDatos_CuandoTodoFunciona() {
        // ARRANGE
        // Simulamos respuesta de Producto
        ProductoResponseDTO producto = new ProductoResponseDTO();
        producto.setId(10L);
        producto.setSku("LAP-123");
        producto.setNombre("Laptop Pro");
        producto.setPrecio(1500.0);
        producto.setCategoriaId(1L);
        when(productoClient.obtenerProductoPorId(10L)).thenReturn(producto);

        // Simulamos respuesta de Categoría
        CategoriaResponseDTO categoria = new CategoriaResponseDTO();
        categoria.setId(1L);
        categoria.setNombre("Computación");
        when(categoriaClient.obtenerCategoriaPorId(1L)).thenReturn(categoria);

        // Simulamos respuesta de Stock (2 sucursales con stock)
        StockResponseDTO stockSucursal1 = new StockResponseDTO();
        stockSucursal1.setCantidadDisponible(10);
        StockResponseDTO stockSucursal2 = new StockResponseDTO();
        stockSucursal2.setCantidadDisponible(5);
        when(stockClient.obtenerStockPorProducto(10L)).thenReturn(List.of(stockSucursal1, stockSucursal2));

        // ACT
        CatalogoDashboardDTO resultado = bffService.obtenerVistaCatalogo(10L);

        // ASSERT
        assertNotNull(resultado);
        assertEquals("LAP-123", resultado.getSku());
        assertEquals("Laptop Pro", resultado.getNombreProducto());
        assertEquals(1500.0, resultado.getPrecio());
        assertEquals("Computación", resultado.getNombreCategoria());
        // Verificamos que tu lógica de sumar stock (10 + 5) funciona perfectamente
        assertEquals(15, resultado.getStockTotalDisponible());

        verify(productoClient, times(1)).obtenerProductoPorId(10L);
        verify(categoriaClient, times(1)).obtenerCategoriaPorId(1L);
        verify(stockClient, times(1)).obtenerStockPorProducto(10L);
    }

    // ====================================================================
    // PRUEBA 2: Resiliencia (Falla el microservicio de Categorías)
    // ====================================================================
    @Test
    void obtenerVistaCatalogo_DeberiaUsarFallback_CuandoCategoriaFalla() {
        // ARRANGE
        ProductoResponseDTO producto = new ProductoResponseDTO();
        producto.setId(10L);
        producto.setNombre("Laptop Pro");
        producto.setCategoriaId(1L); // Tiene ID, pero el microservicio fallará
        when(productoClient.obtenerProductoPorId(10L)).thenReturn(producto);

        // Simulamos una caída del microservicio de Categorías (Timeout o Error 500)
        when(categoriaClient.obtenerCategoriaPorId(1L)).thenThrow(new RuntimeException("Connection refused"));

        StockResponseDTO stock = new StockResponseDTO();
        stock.setCantidadDisponible(5);
        when(stockClient.obtenerStockPorProducto(10L)).thenReturn(List.of(stock));

        // ACT
        CatalogoDashboardDTO resultado = bffService.obtenerVistaCatalogo(10L);

        // ASSERT
        // Validamos que el try-catch de tu código funcionó y aplicó el string de fallback
        assertEquals("Categoría no disponible", resultado.getNombreCategoria());
        assertEquals("Laptop Pro", resultado.getNombreProducto());
        assertEquals(5, resultado.getStockTotalDisponible());
    }

    // ====================================================================
    // PRUEBA 3: Lógica de Negocio (Producto sin categoría asignada)
    // ====================================================================
    @Test
    void obtenerVistaCatalogo_DeberiaRetornarSinCategoria_CuandoIdEsNull() {
        // ARRANGE
        ProductoResponseDTO producto = new ProductoResponseDTO();
        producto.setId(10L);
        producto.setNombre("Cable USB");
        producto.setCategoriaId(null); // Producto huérfano de categoría
        when(productoClient.obtenerProductoPorId(10L)).thenReturn(producto);

        StockResponseDTO stock = new StockResponseDTO();
        stock.setCantidadDisponible(20);
        when(stockClient.obtenerStockPorProducto(10L)).thenReturn(List.of(stock));

        // ACT
        CatalogoDashboardDTO resultado = bffService.obtenerVistaCatalogo(10L);

        // ASSERT
        assertEquals("Sin Categoría", resultado.getNombreCategoria());
        assertEquals(20, resultado.getStockTotalDisponible());

        // Verificamos que si el ID es null, el BFF es inteligente y NUNCA llama a Feign
        verify(categoriaClient, never()).obtenerCategoriaPorId(anyLong());
    }

    // ====================================================================
    // PRUEBA 4: Lógica de Negocio (Producto sin Stock)
    // ====================================================================
    @Test
    void obtenerVistaCatalogo_DeberiaRetornarCeroStock_CuandoListaVacia() {
        // ARRANGE
        ProductoResponseDTO producto = new ProductoResponseDTO();
        producto.setId(10L);
        producto.setCategoriaId(1L);
        when(productoClient.obtenerProductoPorId(10L)).thenReturn(producto);

        CategoriaResponseDTO categoria = new CategoriaResponseDTO();
        categoria.setNombre("Accesorios");
        when(categoriaClient.obtenerCategoriaPorId(1L)).thenReturn(categoria);

        // El microservicio de stock devuelve una lista vacía
        when(stockClient.obtenerStockPorProducto(10L)).thenReturn(List.of());

        // ACT
        CatalogoDashboardDTO resultado = bffService.obtenerVistaCatalogo(10L);

        // ASSERT
        // Tu Stream().mapToInt().sum() debería ser capaz de reducir una lista vacía a 0
        assertEquals(0, resultado.getStockTotalDisponible());
    }

    // ====================================================================
    // PRUEBA 5: Error Crítico (El microservicio "Core" de Productos falla)
    // ====================================================================
    @Test
    void obtenerVistaCatalogo_DeberiaLanzarExcepcion_CuandoProductoFalla() {
        // ARRANGE
        // Si no podemos obtener la base (el producto), no tiene sentido armar el DTO
        when(productoClient.obtenerProductoPorId(10L)).thenThrow(new RuntimeException("Producto no encontrado o MS apagado"));

        // ACT & ASSERT
        assertThrows(RuntimeException.class, () -> {
            bffService.obtenerVistaCatalogo(10L);
        });

        // Verificamos que al fallar el paso 1, los pasos 2 y 3 nunca se ejecutan, ahorrando recursos de red
        verify(categoriaClient, never()).obtenerCategoriaPorId(anyLong());
        verify(stockClient, never()).obtenerStockPorProducto(anyLong());
    }
}