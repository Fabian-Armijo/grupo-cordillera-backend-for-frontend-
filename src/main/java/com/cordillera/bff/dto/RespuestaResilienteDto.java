package com.cordillera.bff.dto;

public class RespuestaResilienteDto<T> {
    private T data;
    private boolean fromCache;
    private String horaCache;

    // Constructor para cuando la conexión funciona (Datos frescos)
    public RespuestaResilienteDto(T data) {
        this.data = data;
        this.fromCache = false;
        this.horaCache = null;
    }

    // Constructor para cuando el servicio está caído (Datos de caché)
    public RespuestaResilienteDto(T data, String horaCache) {
        this.data = data;
        this.fromCache = true;
        this.horaCache = horaCache;
    }

    // Getters y Setters
    public T getData() { return data; }
    public void setData(T data) { this.data = data; }
    public boolean isFromCache() { return fromCache; }
    public void setFromCache(boolean fromCache) { this.fromCache = fromCache; }
    public String getHoraCache() { return horaCache; }
    public void setHoraCache(String horaCache) { this.horaCache = horaCache; }
}