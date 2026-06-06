package com.cordillera.bff.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import java.util.List;
import java.util.Map;

@FeignClient(name = "ms-kpi", url = "http://localhost:8090/api/kpi")
public interface KpiClient {

    @GetMapping("/definiciones")
    List<Map<String, Object>> obtenerTodosLosKpis();
}
