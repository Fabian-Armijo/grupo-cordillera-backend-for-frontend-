package com.cordillera.bff.client;

import com.cordillera.bff.dto.ProductoResponseDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

// Cliente para Productos
@FeignClient(name = "ms-productos", url = "http://localhost:8082/api/productos")
public interface ProductoClient {
    @GetMapping("/{id}")
    ProductoResponseDTO obtenerProductoPorId(@PathVariable("id") Long id);
}
