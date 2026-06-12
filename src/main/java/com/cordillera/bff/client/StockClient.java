package com.cordillera.bff.client;

import com.cordillera.bff.dto.StockResponseDTO;
import com.cordillera.bff.dto.StockRequestDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.http.ResponseEntity;

import java.util.List;

@FeignClient(name = "ms-stock", url = "http://localhost:8085/api/stock")
public interface StockClient {

    @GetMapping("/producto/{productoId}")
    List<StockResponseDTO> obtenerStockPorProducto(@PathVariable("productoId") Long productoId);

    @PostMapping
    ResponseEntity<Object> inicializarStock(@RequestBody StockRequestDTO stockRequest);

    // 🎯 CORREGIDO: Eliminada la redundancia de /api/stock para que coincida con la URL base del cliente
    @GetMapping("/sucursal/{sucursalId}")
    List<StockResponseDTO> obtenerStockPorSucursal(@PathVariable("sucursalId") Long sucursalId);
}