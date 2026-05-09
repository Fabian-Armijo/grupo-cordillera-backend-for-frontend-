package com.cordillera.bff.dto;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class SucursalRequestDto {
    @NotBlank(message = "El código de sucursal es obligatorio")
    private String codigo;

    @NotBlank(message = "El nombre de la sucursal es obligatorio")
    private String nombre;

    private String direccion;

    @NotBlank(message = "La comuna es obligatoria")
    private String comuna;

    @NotBlank(message = "La región es obligatoria")
    private String region;
}