package com.cordillera.bff.controller;

import com.cordillera.bff.dto.CatalogoDashboardDTO;
import com.cordillera.bff.service.CatalogoBffService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/bff/catalogo")
public class CatalogoBffController {

    @Autowired
    private CatalogoBffService bffService;

    @GetMapping("/vista/{productoId}")
    public ResponseEntity<CatalogoDashboardDTO> obtenerVistaFrontend(@PathVariable Long productoId) {
        return ResponseEntity.ok(bffService.obtenerVistaCatalogo(productoId));
    }

    // NUEVO ENDPOINT (Para llenar la tabla de React)
    @GetMapping("/lista")
    public ResponseEntity<List<CatalogoDashboardDTO>> listarTodoElCatalogo() {
        // Necesitarás tener un método en tu bffService que recolecte la lista de productos
        return ResponseEntity.ok(bffService.listarCatalogoCompleto());
    }
}
