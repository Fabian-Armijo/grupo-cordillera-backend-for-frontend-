package com.cordillera.bff.client;

import com.cordillera.bff.dto.ProductoResponseDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.List;

// Cliente para Productos
@FeignClient(name = "ms-productos", url = "http://localhost:8082/api/productos")
public interface ProductoClient {

    // Método existente para buscar por ID
    @GetMapping("/{id}")
    ProductoResponseDTO obtenerProductoPorId(@PathVariable("id") Long id);

    // --- NUEVO MÉTODO ---
    // Al no poner nada dentro de @GetMapping, hereda la ruta base: /api/productos
    @GetMapping
    List<ProductoResponseDTO> obtenerTodosLosProductos();
}