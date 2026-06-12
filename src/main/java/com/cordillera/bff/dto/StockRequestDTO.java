package com.cordillera.bff.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class StockRequestDTO {
    private Long productoId;
    private Long sucursalId;
    private Integer cantidadDisponible;
    private Integer cantidadReservada;
}
