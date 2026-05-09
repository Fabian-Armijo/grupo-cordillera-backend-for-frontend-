package com.cordillera.bff.client;

import com.cordillera.bff.dto.StockResponseDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.List;

// Cliente para Stock
@FeignClient(name = "ms-stock", url = "http://localhost:8085/api/stock")
public interface StockClient {
    @GetMapping("/producto/{productoId}")
    List<StockResponseDTO> obtenerStockPorProducto(@PathVariable("productoId") Long productoId);
}