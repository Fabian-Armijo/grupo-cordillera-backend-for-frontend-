package com.cordillera.bff.controller;

import com.cordillera.bff.client.KpiClient;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
public class KpiBFFControllerTest {

    @Autowired
    private MockMvc mockMvc;

    // Aquí está el truco: dejamos que Spring levante el controlador y el servicio REAL,
    // y solo mockeamos el cliente Feign que sale a internet.
    @MockitoBean
    private KpiClient mockKpiClient;

    @Test
    @WithMockUser
    public void testGetKpisForDashboard_IntegrationFlow() throws Exception {
        // 1. Datos simulados que el microservicio externo regresaría
        List<Map<String, Object>> mockKpis = new ArrayList<>();
        Map<String, Object> kpi = new HashMap<>();
        kpi.put("nombre", "Ventas Totales");
        kpi.put("valor", 1500000);
        mockKpis.add(kpi);

        // 2. Comportamiento del cliente Feign mockeado
        when(mockKpiClient.obtenerTodosLosKpis()).thenReturn(mockKpis);

        // 3. Ejecutar llamada al BFF: viajará por el controlador y ejecutará el servicio real
        mockMvc.perform(get("/bff/kpis/dashboard")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                // 📦 ABRIMOS EL SOBRE: Buscamos dentro de la propiedad "data"
                .andExpect(jsonPath("$.data[0].nombre").value("Ventas Totales"))
                .andExpect(jsonPath("$.data[0].valor").value(1500000))
                // 🛡️ Opcional pero recomendado: verificamos que el sobre indique que NO es caché
                .andExpect(jsonPath("$.fromCache").value(false));
    }
}