package com.cordillera.bff.client;

import com.cordillera.bff.dto.CategoriaResponseDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

// Cliente para Categorías
@FeignClient(name = "ms-categorias", url = "http://localhost:8083/api/categorias")
public interface CategoriaClient {
    @GetMapping("/{id}")
    CategoriaResponseDTO obtenerCategoriaPorId(@PathVariable("id") Long id);
}
