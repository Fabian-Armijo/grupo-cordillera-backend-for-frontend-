package com.cordillera.bff.service;

import com.cordillera.bff.client.KpiClient;
import com.cordillera.bff.client.SucursalClient;
import com.cordillera.bff.client.VentasClient;
import com.cordillera.bff.dto.SucursalResponseDto;
import com.cordillera.bff.dto.VentaRequestDto;
import com.cordillera.bff.dto.VentaResponseDto;
import com.cordillera.bff.service.VentasBffService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("VentasBffService – Tests unitarios")
class VentasBffServiceTest {

    @Mock
    private VentasClient ventaClient;

    @Mock
    private SucursalClient sucursalClient;

    @Mock
    private KpiClient kpiClient;

    @InjectMocks
    private VentasBffService ventasBffService;

    // ─── listarTodasLasVentas ─────────────────────────────────────────────────

    @Nested
    @DisplayName("listarTodasLasVentas")
    class ListarTodasLasVentas {

        @Test
        @DisplayName("Delega la llamada a VentasClient con los parámetros recibidos")
        void listarVentas_delegaParametros() {
            VentaResponseDto venta = new VentaResponseDto();
            venta.setId(1L);
            venta.setMontoTotal(15_000.0);

            when(ventaClient.listarVentas("ADMIN", 1L, "Bearer token123"))
                    .thenReturn(List.of(venta));

            List<VentaResponseDto> resultado =
                    ventasBffService.listarTodasLasVentas("ADMIN", 1L, "Bearer token123");

            assertThat(resultado).hasSize(1);
            assertThat(resultado.get(0).getMontoTotal()).isEqualTo(15_000.0);
            verify(ventaClient, times(1)).listarVentas("ADMIN", 1L, "Bearer token123");
        }

        @Test
        @DisplayName("Pasa nulls al cliente cuando no hay cabeceras")
        void listarVentas_sinCabeceras_pasaNulls() {
            when(ventaClient.listarVentas(null, null, null)).thenReturn(List.of());

            List<VentaResponseDto> resultado =
                    ventasBffService.listarTodasLasVentas(null, null, null);

            assertThat(resultado).isEmpty();
            verify(ventaClient).listarVentas(null, null, null);
        }
    }

    // ─── procesarVenta ────────────────────────────────────────────────────────

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
            request.setOrigen("POS");

            VentaResponseDto ventaConfirmada = new VentaResponseDto();
            ventaConfirmada.setId(99L);
            ventaConfirmada.setSucursalId(2L);
            ventaConfirmada.setProductoId(10L);
            ventaConfirmada.setCantidad(3);
            ventaConfirmada.setMontoTotal(9_000.0);

            when(ventaClient.crearVenta(request)).thenReturn(ventaConfirmada);

            VentaResponseDto resultado = ventasBffService.procesarVenta(request);

            assertThat(resultado.getId()).isEqualTo(99L);
            assertThat(resultado.getSucursalId()).isEqualTo(2L);

            // Verifica que se llamó al acumulador con la sucursal correcta
            verify(kpiClient, times(1)).acumularProgreso(eq(2L), anyList());
        }

        @Test
        @DisplayName("Usa la sucursalId del request si la respuesta no trae sucursalId")
        void procesarVenta_usaSucursalDelRequestSiResponseEsNull() {
            VentaRequestDto request = new VentaRequestDto();
            request.setSucursalId(5L);
            request.setProductoId(20L);
            request.setCantidad(1);
            request.setMontoTotal(3_000.0);
            request.setOrigen("WEB");

            VentaResponseDto ventaConfirmada = new VentaResponseDto();
            ventaConfirmada.setId(50L);
            ventaConfirmada.setSucursalId(null); // La respuesta no trae sucursal
            ventaConfirmada.setProductoId(20L);
            ventaConfirmada.setCantidad(1);
            ventaConfirmada.setMontoTotal(3_000.0);

            when(ventaClient.crearVenta(request)).thenReturn(ventaConfirmada);

            ventasBffService.procesarVenta(request);

            // Debe usar la sucursalId del request (5L)
            verify(kpiClient, times(1)).acumularProgreso(eq(5L), anyList());
        }

        @Test
        @DisplayName("El ítem enviado a KpiClient contiene productoId, cantidad y montoTotal correctos")
        void procesarVenta_itemVendidoConDatosCorrectos() {
            VentaRequestDto request = new VentaRequestDto();
            request.setSucursalId(3L);
            request.setProductoId(7L);
            request.setCantidad(2);
            request.setMontoTotal(6_000.0);
            request.setOrigen("POS");

            VentaResponseDto ventaConfirmada = new VentaResponseDto();
            ventaConfirmada.setId(77L);
            ventaConfirmada.setSucursalId(3L);
            ventaConfirmada.setProductoId(7L);
            ventaConfirmada.setCantidad(2);
            ventaConfirmada.setMontoTotal(6_000.0);

            when(ventaClient.crearVenta(request)).thenReturn(ventaConfirmada);

            // Capturamos la lista que se envía al acumulador
            @SuppressWarnings("unchecked")
            ArgumentCaptor<List<Map<String, Object>>> captor =
                    ArgumentCaptor.forClass(List.class);

            ventasBffService.procesarVenta(request);

            verify(kpiClient).acumularProgreso(eq(3L), captor.capture());

            List<Map<String, Object>> listaEnviada = captor.getValue();
            assertThat(listaEnviada).hasSize(1);

            Map<String, Object> item = listaEnviada.get(0);
            assertThat(item.get("productoId")).isEqualTo(7L);
            assertThat(item.get("cantidad")).isEqualTo(2);
            assertThat(item.get("montoTotal")).isEqualTo(6_000.0);
        }

        @Test
        @DisplayName("Si KpiClient lanza excepción, la venta igual se retorna sin propagar el error")
        void procesarVenta_kpiClientFalla_ventaIgualSeRetorna() {
            VentaRequestDto request = new VentaRequestDto();
            request.setSucursalId(1L);
            request.setProductoId(3L);
            request.setCantidad(1);
            request.setMontoTotal(2_500.0);
            request.setOrigen("POS");

            VentaResponseDto ventaConfirmada = new VentaResponseDto();
            ventaConfirmada.setId(11L);
            ventaConfirmada.setSucursalId(1L);
            ventaConfirmada.setMontoTotal(2_500.0);

            when(ventaClient.crearVenta(request)).thenReturn(ventaConfirmada);
            when(kpiClient.acumularProgreso(anyLong(), anyList()))
                    .thenThrow(new RuntimeException("ms-kpi caído"));

            // NO debe propagar la excepción
            VentaResponseDto resultado = ventasBffService.procesarVenta(request);

            assertThat(resultado.getId()).isEqualTo(11L);
        }

        @Test
        @DisplayName("Usa productoId y cantidad de la response si están disponibles")
        void procesarVenta_usaDatosDeResponseSiExisten() {
            VentaRequestDto request = new VentaRequestDto();
            request.setSucursalId(4L);
            request.setProductoId(1L);   // Valor del request
            request.setCantidad(1);
            request.setMontoTotal(1_000.0);
            request.setOrigen("WEB");

            VentaResponseDto ventaConfirmada = new VentaResponseDto();
            ventaConfirmada.setId(88L);
            ventaConfirmada.setSucursalId(4L);
            ventaConfirmada.setProductoId(99L); // Valor de la respuesta (distinto)
            ventaConfirmada.setCantidad(5);
            ventaConfirmada.setMontoTotal(5_000.0);

            when(ventaClient.crearVenta(request)).thenReturn(ventaConfirmada);

            @SuppressWarnings("unchecked")
            ArgumentCaptor<List<Map<String, Object>>> captor =
                    ArgumentCaptor.forClass(List.class);

            ventasBffService.procesarVenta(request);

            verify(kpiClient).acumularProgreso(eq(4L), captor.capture());

            Map<String, Object> item = captor.getValue().get(0);
            // Debe priorizar los datos de la respuesta
            assertThat(item.get("productoId")).isEqualTo(99L);
            assertThat(item.get("cantidad")).isEqualTo(5);
            assertThat(item.get("montoTotal")).isEqualTo(5_000.0);
        }
    }

    // ─── obtenerSucursalesParaVenta ───────────────────────────────────────────

    @Nested
    @DisplayName("obtenerSucursalesParaVenta")
    class ObtenerSucursalesParaVenta {

        @Test
        @DisplayName("Delega la llamada a SucursalClient y retorna la lista completa")
        void obtenerSucursales_delegaASucursalClient() {
            SucursalResponseDto suc1 = SucursalResponseDto.builder()
                    .id(1L).nombre("Sucursal Santiago").activa(true).build();
            SucursalResponseDto suc2 = SucursalResponseDto.builder()
                    .id(2L).nombre("Sucursal Valparaíso").activa(true).build();

            when(sucursalClient.listarTodas()).thenReturn(List.of(suc1, suc2));

            List<SucursalResponseDto> resultado =
                    ventasBffService.obtenerSucursalesParaVenta();

            assertThat(resultado).hasSize(2);
            assertThat(resultado.get(0).getNombre()).isEqualTo("Sucursal Santiago");
            assertThat(resultado.get(1).getNombre()).isEqualTo("Sucursal Valparaíso");
        }

        @Test
        @DisplayName("Retorna lista vacía si SucursalClient no encuentra sucursales")
        void obtenerSucursales_listaVacia() {
            when(sucursalClient.listarTodas()).thenReturn(List.of());

            List<SucursalResponseDto> resultado =
                    ventasBffService.obtenerSucursalesParaVenta();

            assertThat(resultado).isEmpty();
        }
    }
}
