package com.cordillera.bff.dto;

import lombok.Data;

@Data
public class StockResponseDTO {
    private Long productoId;
    private Integer cantidadDisponible;
}
