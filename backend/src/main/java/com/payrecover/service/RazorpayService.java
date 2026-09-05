package com.payrecover.service;

import com.payrecover.dto.PaymentDTO;
import com.payrecover.dto.RazorpayConfigResponse;
import com.payrecover.dto.RazorpayOrderRequest;
import com.payrecover.dto.RazorpayOrderResponse;
import com.payrecover.entity.Payment;
import com.payrecover.repository.PaymentRepository;
import com.razorpay.Order;
import com.razorpay.RazorpayClient;
import com.razorpay.RazorpayException;
import com.razorpay.Utils;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.UUID;

@Service
public class RazorpayService {

    private final PaymentRepository paymentRepository;

    @Value("${razorpay.key.id:}")
    private String keyId;

    @Value("${razorpay.key.secret:}")
    private String keySecret;

    @Value("${razorpay.webhook.secret:}")
    private String webhookSecret;

    public RazorpayService(PaymentRepository paymentRepository) {
        this.paymentRepository = paymentRepository;
    }

    public boolean isConfigured() {
        return keyId != null && !keyId.isBlank()
                && keySecret != null && !keySecret.isBlank();
    }

    public RazorpayConfigResponse getConfig() {
        RazorpayConfigResponse config = new RazorpayConfigResponse();
        config.setKeyId(keyId);
        config.setConfigured(isConfigured());
        config.setSimulationMode(!isConfigured());
        return config;
    }

    public RazorpayOrderResponse createOrder(RazorpayOrderRequest request) {
        if (request.getAmount() == null || request.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Amount must be greater than zero");
        }

        String paymentId = "rzpay_" + UUID.randomUUID().toString().substring(0, 8);
        String currency = request.getCurrency() != null ? request.getCurrency() : "INR";
        String orderId = createRazorpayOrder(paymentId, request.getAmount(), currency);

        Payment payment = new Payment();
        payment.setPaymentId(paymentId);
        payment.setAmount(request.getAmount());
        payment.setCurrency(currency);
        payment.setStatus("PENDING");
        payment.setCustomerEmail(request.getCustomerEmail());
        payment.setAttemptCount(1);
        payment.setRazorpayOrderId(orderId);
        payment.setSource("RAZORPAY");
        paymentRepository.save(payment);

        RazorpayOrderResponse response = new RazorpayOrderResponse();
        response.setPaymentId(paymentId);
        response.setRazorpayOrderId(orderId);
        response.setRazorpayKeyId(keyId);
        response.setAmount(request.getAmount());
        response.setCurrency(currency);
        response.setStatus("PENDING");
        response.setSimulationMode(!isConfigured());
        return response;
    }

    public String createRetryOrder(Payment payment) {
        String orderId = createRazorpayOrder(
                payment.getPaymentId(),
                payment.getAmount(),
                payment.getCurrency()
        );
        payment.setRazorpayOrderId(orderId);
        payment.setRazorpayPaymentId(null);
        return orderId;
    }

    public PaymentDTO simulateFailure(String paymentId, String failureReason) {
        Payment payment = paymentRepository.findByPaymentId(paymentId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Payment not found"));

        if (!"RAZORPAY".equalsIgnoreCase(payment.getSource())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Only Razorpay payments can be simulated");
        }

        payment.setStatus("FAILED");
        payment.setFailureReason(failureReason != null ? failureReason : "payment_failed");
        Payment saved = paymentRepository.save(payment);
        return mapToDTO(saved);
    }

    public void handleWebhook(String payload, String signature) {
        verifySignature(payload, signature);

        JSONObject event = new JSONObject(payload);
        String eventType = event.getString("event");
        JSONObject entity = extractEntity(event, eventType);

        if (entity == null) {
            return;
        }

        String orderId = entity.optString("order_id", null);
        if (orderId == null || orderId.isBlank()) {
            return;
        }

        Payment payment = paymentRepository.findByRazorpayOrderId(orderId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Payment not found for order: " + orderId));

        switch (eventType) {
            case "payment.failed" -> {
                payment.setStatus("FAILED");
                payment.setFailureReason(entity.optString("error_description", "payment_failed"));
                payment.setRazorpayPaymentId(entity.optString("id", payment.getRazorpayPaymentId()));
            }
            case "payment.captured", "order.paid" -> {
                payment.setStatus("RECOVERED");
                payment.setFailureReason(null);
                payment.setRazorpayPaymentId(entity.optString("id", payment.getRazorpayPaymentId()));
            }
            default -> {
                return;
            }
        }

        paymentRepository.save(payment);
    }

    private String createRazorpayOrder(String receiptId, BigDecimal amount, String currency) {
        if (!isConfigured()) {
            return "sim_order_" + UUID.randomUUID().toString().substring(0, 12);
        }

        try {
            RazorpayClient client = new RazorpayClient(keyId, keySecret);
            JSONObject orderRequest = new JSONObject();
            orderRequest.put("amount", toPaise(amount));
            orderRequest.put("currency", currency);
            orderRequest.put("receipt", receiptId);
            orderRequest.put("payment_capture", 1);

            Order order = client.orders.create(orderRequest);
            return order.get("id");
        } catch (RazorpayException e) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_GATEWAY, "Failed to create Razorpay order: " + e.getMessage(), e);
        }
    }

    private void verifySignature(String payload, String signature) {
        if (webhookSecret == null || webhookSecret.isBlank()) {
            return;
        }
        if (signature == null || signature.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Missing Razorpay signature");
        }
        try {
            Utils.verifyWebhookSignature(payload, signature, webhookSecret);
        } catch (RazorpayException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid webhook signature", e);
        }
    }

    private JSONObject extractEntity(JSONObject event, String eventType) {
        JSONObject payload = event.optJSONObject("payload");
        if (payload == null) {
            return null;
        }

        if (eventType.startsWith("payment.")) {
            JSONObject paymentWrapper = payload.optJSONObject("payment");
            return paymentWrapper != null ? paymentWrapper.optJSONObject("entity") : null;
        }

        if (eventType.startsWith("order.")) {
            JSONObject orderWrapper = payload.optJSONObject("order");
            return orderWrapper != null ? orderWrapper.optJSONObject("entity") : null;
        }

        return null;
    }

    private int toPaise(BigDecimal amount) {
        return amount.multiply(BigDecimal.valueOf(100))
                .setScale(0, RoundingMode.HALF_UP)
                .intValueExact();
    }

    private PaymentDTO mapToDTO(Payment entity) {
        PaymentDTO dto = new PaymentDTO();
        dto.setId(entity.getId());
        dto.setPaymentId(entity.getPaymentId());
        dto.setAmount(entity.getAmount());
        dto.setCurrency(entity.getCurrency());
        dto.setStatus(entity.getStatus());
        dto.setFailureReason(entity.getFailureReason());
        dto.setCustomerEmail(entity.getCustomerEmail());
        dto.setAttemptCount(entity.getAttemptCount());
        dto.setRazorpayOrderId(entity.getRazorpayOrderId());
        dto.setRazorpayPaymentId(entity.getRazorpayPaymentId());
        dto.setSource(entity.getSource());
        dto.setCreatedAt(entity.getCreatedAt());
        dto.setUpdatedAt(entity.getUpdatedAt());
        return dto;
    }
}
