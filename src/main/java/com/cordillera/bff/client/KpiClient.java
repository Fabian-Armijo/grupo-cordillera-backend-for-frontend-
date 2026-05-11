package com.cordillera.bff.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import java.util.List;
import java.util.Map;

//Cliente para KPI
    @FeignClient(name = "kpi-client", url = "http://localhost:8087/api/kpi")
    public interface KpiClient {
        @GetMapping("/definiciones")
        List<Map<String, Object>> obtenerTodosLosKpis();
    }

