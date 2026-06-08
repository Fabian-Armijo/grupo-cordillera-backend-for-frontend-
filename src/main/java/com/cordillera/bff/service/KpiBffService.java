package com.cordillera.bff.service;

import com.cordillera.bff.client.KpiClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.Map;

@Service
public class KpiBffService {

    @Autowired
    private KpiClient kpiClient;

    public List<Map<String, Object>> getKpisForDashboard() {
        return kpiClient.obtenerTodosLosKpis();
    }
}