package com.cordillera.bff.client;

import com.cordillera.bff.client.fallback.SucursalFallback;
import com.cordillera.bff.config.FeignClientConfig;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import com.cordillera.bff.dto.SucursalResponseDto;

import java.util.List;

@FeignClient(name = "ms-sucursales", url = "http://localhost:8084/api/sucursales", configuration = FeignClientConfig.class, fallback = SucursalFallback.class)
public interface SucursalClient {
    @GetMapping("/{id}")
    SucursalResponseDto obtenerPorId(@PathVariable("id") Long id);

    @GetMapping
    List<SucursalResponseDto> listarTodas();
}