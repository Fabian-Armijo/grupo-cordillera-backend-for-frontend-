package com.cordillera.bff.client;

import com.cordillera.bff.dto.ProductoResponseDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@FeignClient(name = "ms-productos", url = "http://localhost:8082/api/productos")
public interface ProductoClient {

    @GetMapping("/{id}")
    ProductoResponseDTO obtenerProductoPorId(@PathVariable("id") Long id);

    @GetMapping
    List<ProductoResponseDTO> obtenerTodosLosProductos();

    @PostMapping
    Object enviarNuevoProducto(
            @RequestHeader("X-User-Role") String rol,
            @RequestHeader("X-Sucursal-Id") Long sucursalId,
            @RequestBody Object productoRequestPayload
    );

    // 🎯 NUEVO MÉTODO: Mapeamos el PUT de actualización que ya existe en tu ms-productos
    @PutMapping("/{id}")
    Object actualizarProducto(
            @PathVariable("id") Long id,
            @RequestHeader("X-User-Role") String rol,
            @RequestHeader("X-Sucursal-Id") Long sucursalId,
            @RequestBody Object productoRequestPayload
    );

    @GetMapping("/categorias")
    List<Object> obtenerCategorias();
}