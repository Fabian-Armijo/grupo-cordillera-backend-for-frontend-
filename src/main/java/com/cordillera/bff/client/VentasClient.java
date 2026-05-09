package com.cordillera.bff.client;

import com.cordillera.bff.config.FeignClientConfig;
import com.cordillera.bff.dto.VentaRequestDto;
import com.cordillera.bff.dto.VentaResponseDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.List;

@FeignClient(name = "ms-ventas", url = "http://localhost:8081/api/ventas", configuration = FeignClientConfig.class)
public interface VentasClient {
    @PostMapping
    VentaResponseDto crearVenta(@RequestBody VentaRequestDto request);

    @GetMapping
    List<VentaResponseDto> listarTodas();
}