package com.cordillera.bff.controller;

import com.cordillera.bff.service.KpiBffService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/bff/kpi")
public class KpiBFFController {
    @Autowired
    private KpiBffService kpiBffService;
    @GetMapping("/dashboard")
    public List<Map<String, Object>> getDashboardData() {
        return kpiBffService.getKpisForDashboard();
    }
}
