package com.cordillera.bff.controller;

import com.cordillera.bff.service.KpiBffService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map; // Asegúrate de tener este import

@RestController
@RequestMapping("/bff/kpis")
public class KpiBFFController {

    @Autowired
    private KpiBffService kpiBffService;

    @GetMapping("/dashboard")
    // Cambiado de List<Object> a List<Map<String, Object>>
    public List<Map<String, Object>> getKpisForDashboard() {
        return kpiBffService.getKpisForDashboard();
    }
}