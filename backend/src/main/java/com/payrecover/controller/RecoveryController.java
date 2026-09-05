package com.payrecover.controller;

import com.payrecover.dto.RecoveryActionDTO;
import com.payrecover.service.RecoveryExecutionService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/recovery")
public class RecoveryController {

    private final RecoveryExecutionService recoveryExecutionService;

    public RecoveryController(RecoveryExecutionService recoveryExecutionService) {
        this.recoveryExecutionService = recoveryExecutionService;
    }

    @PostMapping("/execute/{paymentId}")
    public ResponseEntity<RecoveryActionDTO> execute(@PathVariable String paymentId) {
        return ResponseEntity.ok(recoveryExecutionService.executeLatestRecommendation(paymentId));
    }

    @GetMapping("/history")
    public ResponseEntity<List<RecoveryActionDTO>> getAllHistory() {
        return ResponseEntity.ok(recoveryExecutionService.getAllHistory());
    }

    @GetMapping("/history/{paymentId}")
    public ResponseEntity<List<RecoveryActionDTO>> getHistory(@PathVariable String paymentId) {
        return ResponseEntity.ok(recoveryExecutionService.getHistory(paymentId));
    }

    @GetMapping("/latest/{paymentId}")
    public ResponseEntity<RecoveryActionDTO> getLatest(@PathVariable String paymentId) {
        RecoveryActionDTO latest = recoveryExecutionService.getLatestRecommendation(paymentId);
        if (latest == null) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.ok(latest);
    }
}
