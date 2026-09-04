package com.payrecover.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;

@RestController
public class RootController {

    @GetMapping("/")
    public Map<String, Object> root() {
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("app", "PayRecover AI");
        response.put("status", "running");
        response.put("health", "/api/health");
        response.put("payments", "/api/payments");
        response.put("frontend", "http://localhost:5173");
        return response;
    }
}
