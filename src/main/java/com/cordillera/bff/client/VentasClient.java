package com.cordillera.bff.client;

import com.cordillera.bff.dto.VentaRequestDto;
import com.cordillera.bff.dto.VentaResponseDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.List;

@FeignClient(name = "ms-ventas", url = "http://localhost:8081/api/ventas")
public interface VentasClient {

    // Llama al GET /api/ventas del micro de ventas
    @GetMapping
    List<VentaResponseDto> listarVentas();

    // Llama al POST /api/ventas del micro de ventas
    @PostMapping
    VentaResponseDto crearVenta(@RequestBody VentaRequestDto request);

    // Llama al GET /api/ventas/origen/{origen}
    @GetMapping("/origen/{origen}")
    List<VentaResponseDto> listarPorOrigen(@PathVariable("origen") String origen);
}