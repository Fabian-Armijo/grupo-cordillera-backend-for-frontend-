package com.cordillera.bff.client;

import com.cordillera.bff.dto.VentaRequestDto;
import com.cordillera.bff.dto.VentaResponseDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@FeignClient(name = "ms-ventas", url = "http://localhost:8081/api/ventas")
public interface VentasClient {

    // 🎯 Hacemos las cabeceras opcionales para que soporte llamadas con o sin argumentos
    @GetMapping
    List<VentaResponseDto> listarVentas(
            @RequestHeader(value = "X-User-Role", required = false) String userRole,
            @RequestHeader(value = "X-Sucursal-Id", required = false) Long sucursalId,
            @RequestHeader(value = "Authorization", required = false) String token
    );

    @PostMapping
    VentaResponseDto crearVenta(@RequestBody VentaRequestDto request);
}