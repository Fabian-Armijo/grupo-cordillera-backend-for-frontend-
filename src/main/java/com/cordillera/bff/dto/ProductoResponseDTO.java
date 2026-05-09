package com.cordillera.bff.dto;

import lombok.Data;

@Data
public class ProductoResponseDTO {
    private Long id;
    private String sku;
    private String nombre;
    private Double precio;
    private Long categoriaId;
}
