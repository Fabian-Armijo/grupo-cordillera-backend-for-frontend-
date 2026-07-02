package com.cordillera.bff.service;

import com.cordillera.bff.client.KpiClient;
import com.cordillera.bff.client.SucursalClient;
import com.cordillera.bff.client.VentasClient;
import com.cordillera.bff.dto.RespuestaResilienteDto;
import com.cordillera.bff.dto.SucursalResponseDto;
import com.cordillera.bff.dto.VentaRequestDto;
import com.cordillera.bff.dto.VentaResponseDto;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("VentasBffService – Tests unitarios")
class VentasBFFServiceTest {

    @Mock
    private VentasClient ventaClient;

    @Mock
    private SucursalClient sucursalClient;

    @Mock
    private KpiClient kpiClient;

    @Mock
    private CacheManager cacheManager; // 👈 Agregamos el mock del gestor de caché

    @Mock
    private Cache cache; // 👈 Agregamos el mock de la caché individual

    @InjectMocks
    private VentasBffService ventasBffService;

    // ─── listarTodasLasVentas (AHORA CON SOBRE Y CACHÉ MANAGER) ─────────────

    @Nested
    @DisplayName("listarTodasLasVentas")
    class ListarTodasLasVentas {

        @Test
        @DisplayName("Delega a VentasClient, guarda en caché y devuelve sobre con datos frescos")
        void listarVentas_exitoYGuardaEnCache() {
            VentaResponseDto venta = new VentaResponseDto();
            venta.setId(1L);
            venta.setMontoTotal(15_000.0);

            when(cacheManager.getCache("ventasCache")).thenReturn(cache);
            when(ventaClient.listarVentas("ADMIN", 1L, "Bearer token123"))
                    .thenReturn(List.of(venta));

            // 📦 Recibimos el sobre
            RespuestaResilienteDto<List<VentaResponseDto>> respuesta =
                    ventasBffService.listarTodasLasVentas("ADMIN", 1L, "Bearer token123");

            // 🔓 Extraemos los datos
            assertThat(respuesta.getData()).hasSize(1);
            assertThat(respuesta.getData().get(0).getMontoTotal()).isEqualTo(15_000.0);

            // Verificamos que se guardaron los datos y la hora en la caché
            verify(cache, times(2)).put(anyString(), any());
        }

        @Test
        @DisplayName("VentasClient falla: Rescata los datos vivos de la caché")
        void listarVentas_fallaYRescataCache() {
            when(cacheManager.getCache("ventasCache")).thenReturn(cache);
            when(ventaClient.listarVentas(any(), any(), any())).thenThrow(new RuntimeException("ms-ventas caído"));

            // Simulamos los Wrappers que usa Spring internamente para devolver la caché
            Cache.ValueWrapper mockDataWrapper = mock(Cache.ValueWrapper.class);
            Cache.ValueWrapper mockTimeWrapper = mock(Cache.ValueWrapper.class);

            when(cache.get(anyString())).thenAnswer(invocation -> {
                String key = invocation.getArgument(0);
                if (key.contains("hora")) return mockTimeWrapper;
                return mockDataWrapper;
            });

            VentaResponseDto ventaRescatada = new VentaResponseDto();
            ventaRescatada.setMontoTotal(5000.0);

            when(mockDataWrapper.get()).thenReturn(List.of(ventaRescatada));
            when(mockTimeWrapper.get()).thenReturn("14:30");

            RespuestaResilienteDto<List<VentaResponseDto>> respuesta =
                    ventasBffService.listarTodasLasVentas(null, null, null);

            assertThat(respuesta.getData()).hasSize(1);
            assertThat(respuesta.getData().get(0).getMontoTotal()).isEqualTo(5000.0);
        }

        @Test
        @DisplayName("VentasClient falla y no hay caché: Retorna sobre vacío de seguridad")
        void listarVentas_fallaYSinCache() {
            when(cacheManager.getCache("ventasCache")).thenReturn(cache);
            when(ventaClient.listarVentas(any(), any(), any())).thenThrow(new RuntimeException("ms-ventas caído"));
            when(cache.get(anyString())).thenReturn(null);

            RespuestaResilienteDto<List<VentaResponseDto>> respuesta =
                    ventasBffService.listarTodasLasVentas(null, null, null);

            assertThat(respuesta.getData()).isEmpty();
        }
    }

    // ─── procesarVenta (SIN CAMBIOS, DIRECTO DE TU CÓDIGO REAL) ──────────────

    @Nested
    @DisplayName("procesarVenta")
    class ProcesarVenta {

        @Test
        @DisplayName("Confirma la venta y acumula progreso en KpiClient")
        void procesarVenta_confirmaYAcumula() {
            VentaRequestDto request = new VentaRequestDto();
            request.setSucursalId(2L);
            request.setProductoId(10L);
            request.setCantidad(3);
            request.setMontoTotal(9_000.0);

            VentaResponseDto ventaConfirmada = new VentaResponseDto();
            ventaConfirmada.setId(99L);
            ventaConfirmada.setSucursalId(2L);
            ventaConfirmada.setProductoId(10L);
            ventaConfirmada.setCantidad(3);
            ventaConfirmada.setMontoTotal(9_000.0);

            when(ventaClient.crearVenta(request)).thenReturn(ventaConfirmada);

            VentaResponseDto resultado = ventasBffService.procesarVenta(request);

            assertThat(resultado.getId()).isEqualTo(99L);
            verify(kpiClient, times(1)).acumularProgreso(eq(2L), anyList());
        }

        @Test
        @DisplayName("El ítem enviado a KpiClient contiene productoId, cantidad y montoTotal correctos")
        void procesarVenta_itemVendidoConDatosCorrectos() {
            VentaRequestDto request = new VentaRequestDto();
            request.setSucursalId(3L);
            request.setProductoId(7L);
            request.setCantidad(2);
            request.setMontoTotal(6_000.0);

            VentaResponseDto ventaConfirmada = new VentaResponseDto();
            ventaConfirmada.setId(77L);
            ventaConfirmada.setSucursalId(3L);
            ventaConfirmada.setProductoId(7L);
            ventaConfirmada.setCantidad(2);
            ventaConfirmada.setMontoTotal(6_000.0);

            when(ventaClient.crearVenta(request)).thenReturn(ventaConfirmada);

            @SuppressWarnings("unchecked")
            ArgumentCaptor<List<Map<String, Object>>> captor = ArgumentCaptor.forClass(List.class);

            ventasBffService.procesarVenta(request);

            verify(kpiClient).acumularProgreso(eq(3L), captor.capture());

            Map<String, Object> item = captor.getValue().get(0);
            assertThat(item.get("productoId")).isEqualTo(7L);
            assertThat(item.get("cantidad")).isEqualTo(2);
            assertThat(item.get("montoTotal")).isEqualTo(6_000.0);
        }

        @Test
        @DisplayName("Si KpiClient lanza excepción, la venta igual se retorna sin propagar el error")
        void procesarVenta_kpiClientFalla_ventaIgualSeRetorna() {
            VentaRequestDto request = new VentaRequestDto();
            request.setSucursalId(1L);

            VentaResponseDto ventaConfirmada = new VentaResponseDto();
            ventaConfirmada.setId(11L);
            ventaConfirmada.setSucursalId(1L);

            when(ventaClient.crearVenta(request)).thenReturn(ventaConfirmada);
            when(kpiClient.acumularProgreso(anyLong(), anyList()))
                    .thenThrow(new RuntimeException("ms-kpi caído"));

            VentaResponseDto resultado = ventasBffService.procesarVenta(request);

            assertThat(resultado.getId()).isEqualTo(11L);
        }
    }

    // ─── obtenerSucursalesParaVenta (SIN SOBRE EN TU CÓDIGO) ──────────────────

    @Nested
    @DisplayName("obtenerSucursalesParaVenta")
    class ObtenerSucursalesParaVenta {

        @Test
        @DisplayName("Delega la llamada a SucursalClient y retorna la lista completa")
        void obtenerSucursales_delegaASucursalClient() {
            SucursalResponseDto suc1 = SucursalResponseDto.builder()
                    .id(1L).nombre("Sucursal Santiago").build();

            when(sucursalClient.listarTodas()).thenReturn(List.of(suc1));

            // Fíjate que aquí no abrimos ningún sobre, usamos la lista directa
            List<SucursalResponseDto> resultado = ventasBffService.obtenerSucursalesParaVenta();

            assertThat(resultado).hasSize(1);
            assertThat(resultado.get(0).getNombre()).isEqualTo("Sucursal Santiago");
        }

        @Test
        @DisplayName("Retorna lista vacía si SucursalClient no encuentra sucursales")
        void obtenerSucursales_listaVacia() {
            when(sucursalClient.listarTodas()).thenReturn(List.of());

            List<SucursalResponseDto> resultado = ventasBffService.obtenerSucursalesParaVenta();

            assertThat(resultado).isEmpty();
        }
    }
}