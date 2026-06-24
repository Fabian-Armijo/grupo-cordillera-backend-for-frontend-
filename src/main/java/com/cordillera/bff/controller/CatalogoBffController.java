package com.cordillera.bff.controller;

import com.cordillera.bff.client.CategoriaClient;
import com.cordillera.bff.client.ProductoClient;
import com.cordillera.bff.client.StockClient;
import com.cordillera.bff.dto.CatalogoDashboardDTO;
import com.cordillera.bff.dto.CategoriaResponseDTO;
import com.cordillera.bff.dto.StockRequestDTO;
import com.cordillera.bff.dto.RespuestaResilienteDto; // 👈 ¡El Sobre!
import com.cordillera.bff.service.CatalogoBffService;

// Importaciones de Swagger / OpenAPI
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/bff/catalogo") // 🎯 El único punto de entrada para el catálogo de inventario
@Tag(name = "BFF - Catálogo y Stock", description = "Orquestador unificado de inventario. Combina datos de ms-productos, ms-categorias y ms-stock con tolerancia a fallos.")
public class CatalogoBffController {

    @Autowired
    private CatalogoBffService bffService;

    @Autowired
    private CategoriaClient categoriaClient; // 🎯 Conexión puerto 8083

    @Autowired
    private ProductoClient productoClient; // 🎯 Conexión puerto 8082

    @Autowired
    private StockClient stockClient; // 🎯 Conexión puerto 8085 para inyectar inventario

    // --- 🛒 LISTADO DE PRODUCTOS CON STOCK (100% DINÁMICO Y RESILIENTE) ---
    @Operation(
            summary = "Listar catálogo con stock consolidado",
            description = "Obtiene los productos y sus stocks cruzados. Cuenta con tolerancia a fallos: si un microservicio cae, retorna los últimos datos conocidos en caché envueltos en un DTO resiliente."
    )
    @ApiResponse(responseCode = "200", description = "Catálogo obtenido con éxito (en vivo o desde caché)")
    @GetMapping("/lista")
    public ResponseEntity<RespuestaResilienteDto<List<CatalogoDashboardDTO>>> obtenerListaCatalogoParaVentas(
            @Parameter(hidden = true) @RequestHeader(value = "X-Sucursal-Id", required = false) Long sucursalIdHeader) {

        System.out.println("📦 [GATEWAY-BFF] -> Orquestando catálogo dinámico. Header 'X-Sucursal-Id' capturado: " + sucursalIdHeader);
        try {
            // 🚀 SOLUCIONADO: El servicio ahora devuelve el "Sobre" con la data y el estado de la caché
            RespuestaResilienteDto<List<CatalogoDashboardDTO>> catalogo = bffService.listarCatalogoCompleto(sucursalIdHeader);
            return ResponseEntity.ok(catalogo);
        } catch (Exception e) {
            System.err.println("❌ [GATEWAY-BFF] Error crítico al consolidar datos: " + e.getMessage());
            // Si todo falla a nivel de Gateway, devolvemos un sobre vacío por seguridad
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new RespuestaResilienteDto<>(List.of()));
        }
    }

    // --- 🏷️ LISTADO DE CATEGORÍAS PARA EL CREAR PRODUCTO ---
    @Operation(
            summary = "Obtener lista de categorías",
            description = "Consulta al ms-categorias para desplegar el selector al momento de crear un nuevo producto en el catálogo."
    )
    @ApiResponse(responseCode = "200", description = "Lista de categorías recuperada con éxito")
    @GetMapping("/categorias")
    public ResponseEntity<?> obtenerCategoriasParaModal() {
        System.out.println("🏷️ [GATEWAY-BFF] -> Solicitando categorías al ms-categorias (Puerto 8083) para el catálogo...");
        try {
            List<CategoriaResponseDTO> categorias = categoriaClient.obtenerTodasLasCategorias();
            return ResponseEntity.ok(categorias);
        } catch (Exception e) {
            System.err.println("❌ [GATEWAY-BFF] Error al conectar con ms-categorias: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "Error al consolidar las categorías desde el microservicio."));
        }
    }

    // --- 🚀 CREACIÓN UNIFICADA: CREA EL PRODUCTO E INICIALIZA EL STOCK AL INSTANTE ---
    @Operation(
            summary = "Crear producto y asignar stock inicial",
            description = "Orquesta la creación de un registro maestro en ms-productos y luego inyecta asíncronamente el inventario inicial en ms-stock."
    )
    @ApiResponse(responseCode = "201", description = "Producto y stock creados exitosamente")
    @PostMapping("/crear")
    public ResponseEntity<?> crearProductoUnificado(
            @Parameter(description = "Datos combinados del producto y su inventario inicial") @RequestBody Object productoPayload) {

        System.out.println("🚀 [GATEWAY-BFF] -> Paso 1: Solicitando registro maestro en ms-productos...");

        // 1. Extraer el rol real del usuario logueado de forma dinámica
        String rolUsuario = "USER";
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth != null && !auth.getAuthorities().isEmpty()) {
                String autoridad = auth.getAuthorities().iterator().next().getAuthority();
                rolUsuario = autoridad.replace("ROLE_", "");
            }
        } catch (Exception e) {
            System.err.println("⚠️ No se pudo procesar el rol del contexto: " + e.getMessage());
        }

        // 2. Extraer los datos críticos del payload de React (sucursalId y cantidadDisponible)
        Long sucursalId = null;
        Integer cantidadInicial = 0;

        if (productoPayload instanceof Map) {
            Map<?, ?> map = (Map<?, ?>) productoPayload;
            if (map.containsKey("sucursalId") && map.get("sucursalId") != null) {
                sucursalId = Long.valueOf(map.get("sucursalId").toString());
            }
            if (map.containsKey("cantidadDisponible") && map.get("cantidadDisponible") != null) {
                cantidadInicial = Integer.valueOf(map.get("cantidadDisponible").toString());
            }
        }

        if (sucursalId == null) {
            System.err.println("❌ [GATEWAY-BFF] Error: Falta 'sucursalId' en la petición.");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("message", "No se especificó la sucursal de destino."));
        }

        try {
            // FASE 1: Se crea el producto en el microservicio correspondiente (ms-productos)
            Object respuestaProducto = productoClient.enviarNuevoProducto(rolUsuario, sucursalId, productoPayload);
            System.out.println("✅ [GATEWAY-BFF] -> Producto creado exitosamente en ms-productos.");

            // FASE 2: Si el producto se creó bien, extraemos su ID generado para crear el inventario en ms-stock
            if (respuestaProducto instanceof Map) {
                Map<?, ?> productoCreadoMap = (Map<?, ?>) respuestaProducto;

                if (productoCreadoMap.containsKey("id") && productoCreadoMap.get("id") != null) {
                    Long productoIdGenerado = Long.valueOf(productoCreadoMap.get("id").toString());

                    System.out.println("📦 [GATEWAY-BFF] -> Paso 2: Orquestando stock inicial (" + cantidadInicial
                            + " u.) para Producto ID: " + productoIdGenerado + " en Sucursal: " + sucursalId);

                    // Construimos el DTO que espera tu StockClient
                    StockRequestDTO stockRequest = new StockRequestDTO();
                    stockRequest.setProductoId(productoIdGenerado);
                    stockRequest.setSucursalId(sucursalId);
                    stockRequest.setCantidadDisponible(cantidadInicial);

                    // Impactamos ms-stock (Puerto 8085)
                    stockClient.inicializarStock(stockRequest);
                    System.out.println("⭐ [GATEWAY-BFF] -> Inventario sincronizado en ms-stock de forma transparente.");
                }
            }

            // Retornamos el objeto final al front para que cierre el modal con éxito
            return ResponseEntity.status(HttpStatus.CREATED).body(respuestaProducto);

        } catch (Exception e) {
            System.err.println("❌ [GATEWAY-BFF] Error crítico en la orquestación del catálogo: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "Error al orquestar el producto y su stock: " + e.getMessage()));
        }
    }
}