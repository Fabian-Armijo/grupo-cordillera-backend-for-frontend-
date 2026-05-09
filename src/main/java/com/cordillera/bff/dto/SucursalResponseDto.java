package com.cordillera.bff.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@AllArgsConstructor // <--- ESTA ES LA QUE TE FALTA
@NoArgsConstructor  // <--- Importante para que Spring pueda serializar/deserializar
@Builder

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