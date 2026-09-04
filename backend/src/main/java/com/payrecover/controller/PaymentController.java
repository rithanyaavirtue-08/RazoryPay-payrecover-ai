package com.payrecover.controller;

import com.payrecover.dto.PaymentDTO;
import com.payrecover.dto.RevenueAtRiskDTO;
import com.payrecover.service.PaymentService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
public class PaymentController {

    private final PaymentService paymentService;

    public PaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @PostMapping("/api/payments")
    public ResponseEntity<PaymentDTO> createPayment(@RequestBody PaymentDTO paymentDTO) {
        PaymentDTO created = paymentService.createPayment(paymentDTO);
        return new ResponseEntity<>(created, HttpStatus.CREATED);
    }

    @GetMapping("/api/payments")
    public ResponseEntity<List<PaymentDTO>> getAllPayments() {
        return ResponseEntity.ok(paymentService.getAllPayments());
    }

    @GetMapping("/api/payments/{id}")
    public ResponseEntity<PaymentDTO> getPaymentById(@PathVariable Long id) {
        return ResponseEntity.ok(paymentService.getPaymentById(id));
    }

    @GetMapping("/api/revenue-at-risk")
    public ResponseEntity<RevenueAtRiskDTO> getRevenueAtRisk() {
        return ResponseEntity.ok(paymentService.calculateRevenueAtRisk());
    }
}
