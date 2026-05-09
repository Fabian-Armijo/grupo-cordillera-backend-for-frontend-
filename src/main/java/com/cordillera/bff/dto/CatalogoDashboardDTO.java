package com.cordillera.bff.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class CatalogoDashboardDTO {
    // Datos del microservicio de Productos
    private String sku;
    private String nombreProducto;
    private Double precio;

    // Dato del microservicio de Categorías
    private String nombreCategoria;

    // Dato del microservicio de Stock
    private Integer stockTotalDisponible;
}
