package com.cordillera.bff.service;

import com.cordillera.bff.client.VentasClient;
import com.cordillera.bff.client.SucursalClient; // Asumiendo que existe para las sucursales
import com.cordillera.bff.dto.VentaRequestDto;
import com.cordillera.bff.dto.VentaResponseDto;
import com.cordillera.bff.dto.SucursalResponseDto;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class VentasBffService {

    @Autowired
    private VentasClient ventaClient;

    @Autowired
    private SucursalClient sucursalClient;

    /**
     * Obtiene todas las ventas llamando al microservicio de Ventas.
     * Este es el método que soluciona tu error 404 en la tabla.
     */
    public List<VentaResponseDto> listarTodasLasVentas() {
        return ventaClient.listarVentas();
    }

    /**
     * Procesa una nueva venta.
     */
    public VentaResponseDto procesarVenta(VentaRequestDto request) {
        return ventaClient.crearVenta(request);
    }

    /**
     * Obtiene sucursales activas (útil para selects en el frontend).
     */
    public List<SucursalResponseDto> obtenerSucursalesParaVenta() {
        return sucursalClient.listarTodas();
    }

    // Si necesitaras validar una sucursal antes de enviarla al micro de ventas:
    public SucursalResponseDto verificarSucursal(Long id) {
        return sucursalClient.obtenerPorId(id);
    }
}