package com.cordillera.bff.controller;

import com.cordillera.bff.dto.RespuestaResilienteDto;
import com.cordillera.bff.dto.VentaRequestDto;
import com.cordillera.bff.dto.VentaResponseDto;
import com.cordillera.bff.dto.SucursalResponseDto;
import com.cordillera.bff.service.VentasBffService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;

import java.util.List;

@RestController
@RequestMapping("/bff/ventas")
public class VentaBffController {

    @Autowired
    private VentasBffService vBffService;

    /**
     * Responde a GET http://localhost:8090/bff/ventas
     * Captura las cabeceras directamente desde la petición de React (igual que en KPI)
     * y las delega al servicio encargado de orquestar la llamada a ms-ventas.
     */
    @GetMapping
    public ResponseEntity<RespuestaResilienteDto<List<VentaResponseDto>>> listarTodas(
            @Parameter(hidden = true) @RequestHeader(value = "X-User-Role", required = false) String userRole,
            @Parameter(hidden = true) @RequestHeader(value = "X-Sucursal-Id", required = false) Long sucursalId,
            @Parameter(hidden = true) @RequestHeader(value = "Authorization", required = false) String token) {

        // Enviamos las credenciales capturadas directo al flujo de negocio del Service
        return ResponseEntity.ok(vBffService.listarTodasLasVentas(userRole, sucursalId, token));
    }

    /**
     * Responde a POST http://localhost:8090/bff/ventas/confirmar
     */
    @PostMapping("/confirmar")
    public ResponseEntity<VentaResponseDto> confirmarVenta(@RequestBody VentaRequestDto request) {
        return ResponseEntity.ok(vBffService.procesarVenta(request));
    }

    /**
     * Responde a GET http://localhost:8090/bff/ventas/sucursales-activas
     */
    @GetMapping("/sucursales-activas")
    public ResponseEntity<List<SucursalResponseDto>> getSucursales() {
        return ResponseEntity.ok(vBffService.obtenerSucursalesParaVenta());
    }
}