package com.payrecover.controller;

import com.payrecover.dto.PaymentDTO;
import com.payrecover.dto.RazorpayConfigResponse;
import com.payrecover.dto.RazorpayOrderRequest;
import com.payrecover.dto.RazorpayOrderResponse;
import com.payrecover.dto.SimulateFailureRequest;
import com.payrecover.service.RazorpayService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/razorpay")
public class RazorpayController {

    private final RazorpayService razorpayService;

    public RazorpayController(RazorpayService razorpayService) {
        this.razorpayService = razorpayService;
    }

    @GetMapping("/config")
    public ResponseEntity<RazorpayConfigResponse> getConfig() {
        return ResponseEntity.ok(razorpayService.getConfig());
    }

    @PostMapping("/orders")
    public ResponseEntity<RazorpayOrderResponse> createOrder(@RequestBody RazorpayOrderRequest request) {
        return new ResponseEntity<>(razorpayService.createOrder(request), HttpStatus.CREATED);
    }

    @PostMapping("/webhook")
    public ResponseEntity<Void> handleWebhook(
            @RequestBody String payload,
            @RequestHeader(value = "X-Razorpay-Signature", required = false) String signature) {
        razorpayService.handleWebhook(payload, signature);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/simulate-failure/{paymentId}")
    public ResponseEntity<PaymentDTO> simulateFailure(
            @PathVariable String paymentId,
            @RequestBody(required = false) SimulateFailureRequest request) {
        String reason = request != null ? request.getFailureReason() : "payment_failed";
        return ResponseEntity.ok(razorpayService.simulateFailure(paymentId, reason));
    }
}
