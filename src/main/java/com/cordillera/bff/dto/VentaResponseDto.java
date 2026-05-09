package com.cordillera.bff.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VentaResponseDto {
    private Long id;
    private String nombreSucursal;
    private String nombreProducto; // Sacas esto de venta.getProducto().getNombre()
    private String skuProducto;
    private Integer cantidad;
    private Double montoTotal;
    private String fechaFormateada;
    private String origen;
    private Long productoId;   // Campo faltante
    private Long sucursalId;
    private LocalDateTime fechaVenta;
}