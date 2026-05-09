package com.cordillera.bff.client.fallback;


import com.cordillera.bff.client.SucursalClient;
import com.cordillera.bff.dto.SucursalResponseDto;
import org.springframework.stereotype.Component;
import java.util.Collections;
import java.util.List;

@Component
public class SucursalFallback implements SucursalClient {

    @Override
    public SucursalResponseDto obtenerPorId(Long id) {
        return SucursalResponseDto.builder()
                .id(id)
                .nombre("Servicio de Sucursales no disponible")
                .codigo("N/A")
                .direccion("Temporalmente fuera de línea")
                .comuna("N/A")
                .region("N/A")
                .activa(false)
                .build();
    }

    @Override
    public List<SucursalResponseDto> listarTodas() {
        SucursalResponseDto fallback = SucursalResponseDto.builder()
                .nombre("SERVICIO NO DISPONIBLE")
                .direccion("Intente más tarde")
                .build();
        return List.of(fallback); // <--- Ahora verás un objeto con el mensaje dentro de la lista
    }
}