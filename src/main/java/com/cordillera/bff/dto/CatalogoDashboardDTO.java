package com.cordillera.bff.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CatalogoDashboardDTO {

    private Long id; // 🎯 El ID numérico real de la base de datos (Ej: 11)
    private String sku; // 🎯 El código autogenerado (Ej: ALMRAM-11)
    private String nombreProducto; // Coincide con p.nombreProducto en tu React
    private Double precio;
    private String nombreCategoria;
    private Integer stockTotalDisponible; // Coincide con p.stockTotalDisponible en tu React
}