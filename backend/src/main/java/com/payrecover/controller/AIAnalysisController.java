package com.payrecover.controller;

import com.payrecover.dto.AIAnalysisResponse;
import com.payrecover.service.AIAnalysisService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/ai/analyze")
public class AIAnalysisController {

    private final AIAnalysisService aiAnalysisService;

    public AIAnalysisController(AIAnalysisService aiAnalysisService) {
        this.aiAnalysisService = aiAnalysisService;
    }

    @PostMapping("/{paymentId}")
    public ResponseEntity<AIAnalysisResponse> analyzePayment(@PathVariable String paymentId) {
        return ResponseEntity.ok(aiAnalysisService.analyzePayment(paymentId));
    }
}
