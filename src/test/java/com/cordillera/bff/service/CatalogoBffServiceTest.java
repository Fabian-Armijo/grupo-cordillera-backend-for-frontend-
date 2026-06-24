package com.cordillera.bff.service;

import com.cordillera.bff.client.ProductoClient;
import com.cordillera.bff.client.StockClient;
import com.cordillera.bff.dto.CatalogoDashboardDTO;
import com.cordillera.bff.dto.ProductoResponseDTO;
import com.cordillera.bff.dto.StockResponseDTO;
import com.cordillera.bff.service.CatalogoBffService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("CatalogoBffService – Tests unitarios")
class CatalogoBffServiceTest {

    @Mock
    private ProductoClient productoClient;

    @Mock
    private StockClient stockClient;

    @InjectMocks
    private CatalogoBffService catalogoBffService;

    // ─── Helpers ─────────────────────────────────────────────────────────────

    private StockResponseDTO buildStock(Long productoId, Integer cantidad) {
        StockResponseDTO s = new StockResponseDTO();
        s.setProductoId(productoId);
        s.setCantidadDisponible(cantidad);
        return s;
    }

    private ProductoResponseDTO buildProducto(Long id, String sku, String nombre, Double precio, Long sucursalId) {
        ProductoResponseDTO p = new ProductoResponseDTO();
        p.setId(id);
        p.setSku(sku);
        p.setNombre(nombre);
        p.setPrecio(precio);
        p.setSucursalId(sucursalId);
        return p;
    }

    private void mockSecurityContextVacio() {
        SecurityContext ctx = mock(SecurityContext.class);
        Authentication auth = mock(Authentication.class);
        when(ctx.getAuthentication()).thenReturn(auth);
        when(auth.getPrincipal()).thenReturn("anonymousUser"); // String → no es Map
        when(auth.getDetails()).thenReturn(null);
        SecurityContextHolder.setContext(ctx);
    }

    // ─── listarCatalogoCompleto ───────────────────────────────────────────────

    @Nested
    @DisplayName("listarCatalogoCompleto")
    class ListarCatalogoCompleto {

        @Test
        @DisplayName("Retorna catálogo enriquecido combinando stock y producto correctamente")
        void catalogo_stockYProductoCoinciden_retornaDTOCorrecto() {
            mockSecurityContextVacio();

            when(stockClient.obtenerStockPorSucursal(1L))
                    .thenReturn(List.of(buildStock(10L, 5)));
            when(productoClient.obtenerTodosLosProductos())
                    .thenReturn(List.of(buildProducto(10L, "CAFE-10", "Café Americano", 1500.0, 1L)));

            List<CatalogoDashboardDTO> resultado =
                    catalogoBffService.listarCatalogoCompleto(1L);

            assertThat(resultado).hasSize(1);
            CatalogoDashboardDTO item = resultado.get(0);
            assertThat(item.getId()).isEqualTo(10L);
            assertThat(item.getSku()).isEqualTo("CAFE-10");
            assertThat(item.getNombreProducto()).isEqualTo("Café Americano");
            assertThat(item.getPrecio()).isEqualTo(1500.0);
            assertThat(item.getStockTotalDisponible()).isEqualTo(5);
        }

        @Test
        @DisplayName("Filtra ítems con cantidadDisponible = 0 o null")
        void catalogo_stockCeroONull_seExcluye() {
            mockSecurityContextVacio();

            when(stockClient.obtenerStockPorSucursal(1L)).thenReturn(List.of(
                    buildStock(1L, 0),   // debe excluirse
                    buildStock(2L, null), // debe excluirse
                    buildStock(3L, 10)   // debe incluirse
            ));
            when(productoClient.obtenerTodosLosProductos()).thenReturn(List.of(
                    buildProducto(3L, "SAND-3", "Sándwich", 2500.0, 1L)
            ));

            List<CatalogoDashboardDTO> resultado =
                    catalogoBffService.listarCatalogoCompleto(1L);

            assertThat(resultado).hasSize(1);
            assertThat(resultado.get(0).getId()).isEqualTo(3L);
        }

        @Test
        @DisplayName("Cuando no hay match en productos, usa valores por defecto ('S/N', precio=0.0)")
        void catalogo_productoNoEncontrado_usaValoresPorDefecto() {
            mockSecurityContextVacio();

            when(stockClient.obtenerStockPorSucursal(1L))
                    .thenReturn(List.of(buildStock(99L, 3)));
            when(productoClient.obtenerTodosLosProductos())
                    .thenReturn(List.of()); // sin productos

            List<CatalogoDashboardDTO> resultado =
                    catalogoBffService.listarCatalogoCompleto(1L);

            assertThat(resultado).hasSize(1);
            assertThat(resultado.get(0).getSku()).isEqualTo("S/N");
            assertThat(resultado.get(0).getPrecio()).isEqualTo(0.0);
            assertThat(resultado.get(0).getNombreProducto())
                    .contains("Producto Descatalogado");
        }

        @Test
        @DisplayName("Retorna lista vacía si StockClient devuelve lista vacía")
        void catalogo_stockVacio_retornaListaVacia() {
            mockSecurityContextVacio();

            when(stockClient.obtenerStockPorSucursal(1L)).thenReturn(List.of());

            List<CatalogoDashboardDTO> resultado =
                    catalogoBffService.listarCatalogoCompleto(1L);

            assertThat(resultado).isEmpty();
            verify(productoClient, never()).obtenerTodosLosProductos();
        }

        @Test
        @DisplayName("Retorna lista vacía si StockClient devuelve null")
        void catalogo_stockNull_retornaListaVacia() {
            mockSecurityContextVacio();

            when(stockClient.obtenerStockPorSucursal(1L)).thenReturn(null);

            List<CatalogoDashboardDTO> resultado =
                    catalogoBffService.listarCatalogoCompleto(1L);

            assertThat(resultado).isEmpty();
        }

        @Test
        @DisplayName("Retorna lista vacía si ProductoClient lanza excepción")
        void catalogo_productoClientFalla_retornaListaVacia() {
            mockSecurityContextVacio();

            when(stockClient.obtenerStockPorSucursal(1L))
                    .thenReturn(List.of(buildStock(1L, 10)));
            when(productoClient.obtenerTodosLosProductos())
                    .thenThrow(new RuntimeException("ms-productos caído"));

            List<CatalogoDashboardDTO> resultado =
                    catalogoBffService.listarCatalogoCompleto(1L);

            assertThat(resultado).isEmpty();
        }

        @Test
        @DisplayName("Retorna lista vacía si StockClient lanza excepción")
        void catalogo_stockClientFalla_retornaListaVacia() {
            mockSecurityContextVacio();

            when(stockClient.obtenerStockPorSucursal(1L))
                    .thenThrow(new RuntimeException("ms-stock caído"));

            List<CatalogoDashboardDTO> resultado =
                    catalogoBffService.listarCatalogoCompleto(1L);

            assertThat(resultado).isEmpty();
            verify(productoClient, never()).obtenerTodosLosProductos();
        }

        @Test
        @DisplayName("Usa la sucursalId del header cuando SecurityContext no aporta sucursal")
        void catalogo_usaHeaderSucursalIdSiContextoNoTieneSucursal() {
            mockSecurityContextVacio();

            when(stockClient.obtenerStockPorSucursal(3L))
                    .thenReturn(List.of(buildStock(5L, 2)));
            when(productoClient.obtenerTodosLosProductos())
                    .thenReturn(List.of(buildProducto(5L, "SKU-5", "Agua Mineral", 800.0, 3L)));

            List<CatalogoDashboardDTO> resultado =
                    catalogoBffService.listarCatalogoCompleto(3L); // header = 3L

            verify(stockClient).obtenerStockPorSucursal(3L);
            assertThat(resultado).hasSize(1);
        }

        @Test
        @DisplayName("Usa sucursalId = 7L como fallback cuando no hay contexto ni header")
        void catalogo_sinContextoNiHeader_usaSucursal7() {
            mockSecurityContextVacio();

            when(stockClient.obtenerStockPorSucursal(7L)).thenReturn(List.of());

            catalogoBffService.listarCatalogoCompleto(null); // header = null

            verify(stockClient).obtenerStockPorSucursal(7L);
        }

        @Test
        @DisplayName("SecurityContext con details Map aporta la sucursalId correctamente")
        void catalogo_securityContextConDetailsMap_usaSucursalDelContexto() {
            SecurityContext ctx = mock(SecurityContext.class);
            Authentication auth = mock(Authentication.class);
            when(ctx.getAuthentication()).thenReturn(auth);
            when(auth.getPrincipal()).thenReturn("cajero@cordillera.cl");

            // El details es un Map que contiene sucursalId
            Map<String, Object> details = Map.of("sucursalId", 4L, "username", "cajero@cordillera.cl");
            when(auth.getDetails()).thenReturn(details);
            SecurityContextHolder.setContext(ctx);

            when(stockClient.obtenerStockPorSucursal(4L)).thenReturn(List.of());

            catalogoBffService.listarCatalogoCompleto(99L); // header ignorado

            verify(stockClient).obtenerStockPorSucursal(4L);
        }

        @Test
        @DisplayName("Retorna lista vacía si ProductoClient retorna null")
        void catalogo_productosNull_retornaListaVacia() {
            mockSecurityContextVacio();

            when(stockClient.obtenerStockPorSucursal(1L))
                    .thenReturn(List.of(buildStock(1L, 5)));
            when(productoClient.obtenerTodosLosProductos()).thenReturn(null);

            List<CatalogoDashboardDTO> resultado =
                    catalogoBffService.listarCatalogoCompleto(1L);

            assertThat(resultado).isEmpty();
        }
    }
}