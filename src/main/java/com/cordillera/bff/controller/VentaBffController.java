package com.cordillera.bff.controller;


import com.cordillera.bff.dto.VentaRequestDto;
import com.cordillera.bff.dto.VentaResponseDto;
import com.cordillera.bff.dto.SucursalResponseDto;
import com.cordillera.bff.service.VentasBffService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/bff/ventas")
public class VentaBffController {

    @Autowired
    private VentasBffService vBffService;

    @PostMapping("/confirmar")
    public ResponseEntity<VentaResponseDto> confirmarVenta(@RequestBody VentaRequestDto request) {
        return ResponseEntity.ok(vBffService.procesarVenta(request));
    }

    @GetMapping("/sucursales-activas")
    public ResponseEntity<List<SucursalResponseDto>> getSucursales() {
        return ResponseEntity.ok(vBffService.obtenerSucursalesParaVenta());
    }
}