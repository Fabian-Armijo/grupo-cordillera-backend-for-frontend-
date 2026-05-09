package com.cordillera.bff.service;


import com.cordillera.bff.client.VentasClient;
import com.cordillera.bff.client.SucursalClient;
import com.cordillera.bff.dto.VentaRequestDto;
import com.cordillera.bff.dto.VentaResponseDto;
import com.cordillera.bff.dto.SucursalResponseDto;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class VentasBffService {

    @Autowired
    private VentasClient ventasClient;

    @Autowired
    private SucursalClient sucursalClient;

    // Método para crear venta a través del BFF
    public VentaResponseDto procesarVenta(VentaRequestDto request) {
        // Aquí el BFF podría hacer validaciones extra antes de mandar la venta
        return ventasClient.crearVenta(request);
    }

    // Método para listar sucursales disponibles (útil para que el frontend llene un combobox)
    public List<SucursalResponseDto> obtenerSucursalesParaVenta() {
        return sucursalClient.listarTodas();
    }
}