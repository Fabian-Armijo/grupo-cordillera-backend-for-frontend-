package com.cordillera.bff.dto;

import lombok.Data;

@Data
public class SucursalResponseDto {
    private Long id;
    private String codigo;
    private String nombre;
    private String direccion;
    private String comuna;
    private String region;
    private Boolean activa;
}