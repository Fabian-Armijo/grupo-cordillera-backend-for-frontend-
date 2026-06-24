package com.cordillera.bff.controller;

import com.cordillera.bff.client.CategoriaClient;
import com.cordillera.bff.client.ProductoClient;
import com.cordillera.bff.client.StockClient;
import com.cordillera.bff.dto.CatalogoDashboardDTO;
import com.cordillera.bff.dto.CategoriaResponseDTO;
import com.cordillera.bff.service.CatalogoBffService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CatalogoBffControllerTest {

    @Mock
    private CatalogoBffService bffService;

    @Mock
    private CategoriaClient categoriaClient;

    @Mock
    private ProductoClient productoClient;

    @Mock
    private StockClient stockClient;

    @InjectMocks
    private CatalogoBffController controller;

    @Test
    void obtenerListaCatalogoParaVentas_DeberiaRetornarCatalogo() {

        Long sucursalId = 1L;

        List<CatalogoDashboardDTO> catalogo = List.of(
                new CatalogoDashboardDTO()
        );

        when(bffService.listarCatalogoCompleto(sucursalId))
                .thenReturn(catalogo);

        ResponseEntity<?> response =
                controller.obtenerListaCatalogoParaVentas(sucursalId);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(catalogo, response.getBody());

        verify(bffService).listarCatalogoCompleto(sucursalId);
    }

    @Test
    void obtenerListaCatalogoParaVentas_DeberiaRetornarError500() {

        when(bffService.listarCatalogoCompleto(any()))
                .thenThrow(new RuntimeException("Error"));

        ResponseEntity<?> response =
                controller.obtenerListaCatalogoParaVentas(1L);

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR,
                response.getStatusCode());

        verify(bffService).listarCatalogoCompleto(1L);
    }

    @Test
    void obtenerCategoriasParaModal_DeberiaRetornarCategorias() {

        List<CategoriaResponseDTO> categorias = List.of(
                new CategoriaResponseDTO()
        );

        when(categoriaClient.obtenerTodasLasCategorias())
                .thenReturn(categorias);

        ResponseEntity<?> response =
                controller.obtenerCategoriasParaModal();

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(categorias, response.getBody());

        verify(categoriaClient).obtenerTodasLasCategorias();
    }

    @Test
    void obtenerCategoriasParaModal_DeberiaRetornarError500() {

        when(categoriaClient.obtenerTodasLasCategorias())
                .thenThrow(new RuntimeException("ms-categorias caído"));

        ResponseEntity<?> response =
                controller.obtenerCategoriasParaModal();

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR,
                response.getStatusCode());

        verify(categoriaClient).obtenerTodasLasCategorias();
    }

    @Test
    void crearProductoUnificado_DeberiaCrearProductoYStock() {

        Map<String, Object> payload = Map.of(
                "sucursalId", 1L,
                "cantidadDisponible", 20
        );

        Map<String, Object> productoCreado = Map.of(
                "id", 100L,
                "nombre", "Café"
        );

        when(productoClient.enviarNuevoProducto(
                anyString(),
                eq(1L),
                eq(payload)
        )).thenReturn(productoCreado);

        ResponseEntity<?> response =
                controller.crearProductoUnificado(payload);

        assertEquals(HttpStatus.CREATED,
                response.getStatusCode());

        assertEquals(productoCreado,
                response.getBody());

        verify(productoClient)
                .enviarNuevoProducto(anyString(), eq(1L), eq(payload));

        verify(stockClient)
                .inicializarStock(any());
    }

    @Test
    void crearProductoUnificado_DeberiaRetornarBadRequestCuandoNoHaySucursal() {

        Map<String, Object> payload = Map.of(
                "cantidadDisponible", 10
        );

        ResponseEntity<?> response =
                controller.crearProductoUnificado(payload);

        assertEquals(HttpStatus.BAD_REQUEST,
                response.getStatusCode());

        verifyNoInteractions(productoClient);
        verifyNoInteractions(stockClient);
    }

    @Test
    void crearProductoUnificado_DeberiaRetornarError500() {

        Map<String, Object> payload = Map.of(
                "sucursalId", 1L,
                "cantidadDisponible", 10
        );

        when(productoClient.enviarNuevoProducto(
                anyString(),
                anyLong(),
                any()
        )).thenThrow(new RuntimeException("Error ms-productos"));

        ResponseEntity<?> response =
                controller.crearProductoUnificado(payload);

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR,
                response.getStatusCode());

        verify(productoClient)
                .enviarNuevoProducto(anyString(), anyLong(), any());
    }
}