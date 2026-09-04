package com.payrecover.service;

import com.payrecover.dto.AIAnalysisResponse;
import com.payrecover.entity.Payment;
import com.payrecover.repository.PaymentRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import java.util.Set;

@Service
public class AIAnalysisService {

    private final PaymentRepository paymentRepository;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    @Value("${gemini.api.key}")
    private String apiKey;

    @Value("${gemini.api.url}")
    private String apiUrl;

    @Value("${gemini.api.model}")
    private String apiModel;

    private static final Set<String> ALLOWED_ACTIONS = Set.of("RETRY", "REMINDER", "ESCALATE", "STOP");

    public AIAnalysisService(PaymentRepository paymentRepository) {
        this.paymentRepository = paymentRepository;
        this.restTemplate = new RestTemplate();
        this.objectMapper = new ObjectMapper();
    }

    public AIAnalysisResponse analyzePayment(String paymentId) {
        Payment payment = paymentRepository.findByPaymentId(paymentId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Payment not found"));

        if (!"FAILED".equalsIgnoreCase(payment.getStatus())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Payment is not in FAILED status");
        }

        String prompt = buildPrompt(payment);
        String jsonResponse = callLLM(prompt);

        return validateAndParseResponse(jsonResponse);
    }

    private String buildPrompt(Payment payment) {
        return String.format(
            "Analyze the following failed payment and recommend an action. " +
            "You MUST reply ONLY with a valid JSON object. Do not include markdown blocks or any other text.\n" +
            "The JSON must have this exact format:\n" +
            "{\n" +
            "  \"action\": \"RETRY\" | \"REMINDER\" | \"ESCALATE\" | \"STOP\",\n" +
            "  \"reason\": \"A short explanation\",\n" +
            "  \"confidence\": 0.95\n" +
            "}\n\n" +
            "Payment Details:\n" +
            "- Payment ID: %s\n" +
            "- Amount: %s\n" +
            "- Currency: %s\n" +
            "- Failure Reason: %s\n" +
            "- Attempt Count: %d\n",
            payment.getPaymentId(),
            payment.getAmount(),
            payment.getCurrency(),
            payment.getFailureReason(),
            payment.getAttemptCount()
        );
    }

    private String callLLM(String prompt) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        String fullUrl = apiUrl + apiModel + ":generateContent?key=" + apiKey;

        Map<String, Object> requestBody = Map.of(
                "contents", List.of(
                        Map.of("parts", List.of(
                                Map.of("text", prompt)
                        ))
                ),
                "systemInstruction", Map.of(
                        "parts", List.of(
                                Map.of("text", "You are an AI payment recovery assistant. You only respond in JSON.")
                        )
                ),
                "generationConfig", Map.of(
                        "responseMimeType", "application/json",
                        "temperature", 0.0
                )
        );

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

        try {
            ResponseEntity<Map> response = restTemplate.postForEntity(fullUrl, entity, Map.class);
            Map<String, Object> body = response.getBody();
            if (body != null && body.containsKey("candidates")) {
                List<Map<String, Object>> candidates = (List<Map<String, Object>>) body.get("candidates");
                if (!candidates.isEmpty()) {
                    Map<String, Object> content = (Map<String, Object>) candidates.get(0).get("content");
                    List<Map<String, Object>> parts = (List<Map<String, Object>>) content.get("parts");
                    if (!parts.isEmpty()) {
                        return (String) parts.get(0).get("text");
                    }
                }
            }
            throw new RuntimeException("Invalid response format from Gemini API");
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to call Gemini API", e);
        }
    }

    private AIAnalysisResponse validateAndParseResponse(String json) {
        try {
            // Remove markdown code block if present
            if (json.startsWith("```json")) {
                json = json.substring(7);
                if (json.endsWith("```")) {
                    json = json.substring(0, json.length() - 3);
                }
            } else if (json.startsWith("```")) {
                json = json.substring(3);
                if (json.endsWith("```")) {
                    json = json.substring(0, json.length() - 3);
                }
            }
            json = json.trim();

            AIAnalysisResponse response = objectMapper.readValue(json, AIAnalysisResponse.class);

            if (response.getAction() == null || !ALLOWED_ACTIONS.contains(response.getAction())) {
                return fallbackResponse("Invalid action received from AI");
            }
            if (response.getConfidence() < 0.0 || response.getConfidence() > 1.0) {
                return fallbackResponse("Invalid confidence score received from AI");
            }
            return response;
        } catch (JsonProcessingException e) {
            return fallbackResponse("Failed to parse AI response");
        }
    }

    private AIAnalysisResponse fallbackResponse(String reason) {
        AIAnalysisResponse fallback = new AIAnalysisResponse();
        fallback.setAction("STOP");
        fallback.setReason("Fallback due to AI error: " + reason);
        fallback.setConfidence(0.0);
        return fallback;
    }
}
