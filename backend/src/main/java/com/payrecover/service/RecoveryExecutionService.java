package com.payrecover.service;

import com.payrecover.dto.RecoveryActionDTO;
import com.payrecover.entity.Payment;
import com.payrecover.entity.RecoveryAction;
import com.payrecover.repository.PaymentRepository;
import com.payrecover.repository.RecoveryActionRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class RecoveryExecutionService {

    private final PaymentRepository paymentRepository;
    private final RecoveryActionRepository recoveryActionRepository;

    public RecoveryExecutionService(
            PaymentRepository paymentRepository,
            RecoveryActionRepository recoveryActionRepository) {
        this.paymentRepository = paymentRepository;
        this.recoveryActionRepository = recoveryActionRepository;
    }

    public RecoveryAction saveRecommendation(String paymentId, String action, String reason, double confidence) {
        RecoveryAction recoveryAction = new RecoveryAction();
        recoveryAction.setPaymentId(paymentId);
        recoveryAction.setAction(action);
        recoveryAction.setReason(reason);
        recoveryAction.setConfidence(confidence);
        recoveryAction.setExecutionStatus("RECOMMENDED");
        return recoveryActionRepository.save(recoveryAction);
    }

    public RecoveryActionDTO executeLatestRecommendation(String paymentId) {
        Payment payment = paymentRepository.findByPaymentId(paymentId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Payment not found"));

        RecoveryAction recoveryAction = recoveryActionRepository
                .findFirstByPaymentIdAndExecutionStatusOrderByCreatedAtDesc(paymentId, "RECOMMENDED")
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.BAD_REQUEST, "No pending AI recommendation. Run Analyze first."));

        String message = executeAction(payment, recoveryAction.getAction());

        recoveryAction.setExecutionStatus("EXECUTED");
        recoveryAction.setExecutionMessage(message);
        recoveryAction.setExecutedAt(LocalDateTime.now());
        paymentRepository.save(payment);
        RecoveryAction saved = recoveryActionRepository.save(recoveryAction);

        return mapToDTO(saved);
    }

    public List<RecoveryActionDTO> getHistory(String paymentId) {
        return recoveryActionRepository.findByPaymentIdOrderByCreatedAtDesc(paymentId).stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    public List<RecoveryActionDTO> getAllHistory() {
        return recoveryActionRepository.findAllByOrderByCreatedAtDesc().stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    public RecoveryActionDTO getLatestRecommendation(String paymentId) {
        return recoveryActionRepository
                .findFirstByPaymentIdAndExecutionStatusOrderByCreatedAtDesc(paymentId, "RECOMMENDED")
                .map(this::mapToDTO)
                .orElse(null);
    }

    private String executeAction(Payment payment, String action) {
        return switch (action) {
            case "RETRY" -> executeRetry(payment);
            case "REMINDER" -> executeReminder(payment);
            case "ESCALATE" -> executeEscalate(payment);
            case "STOP" -> executeStop(payment);
            default -> throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unknown action: " + action);
        };
    }

    private String executeRetry(Payment payment) {
        int attempts = payment.getAttemptCount() + 1;
        payment.setAttemptCount(attempts);

        if (attempts >= 3) {
            payment.setStatus("STOPPED");
            return "Max retry attempts reached (" + attempts + "). Payment marked as STOPPED.";
        }

        payment.setStatus("PENDING");
        return "Retry #" + attempts + " initiated for payment " + payment.getPaymentId() + ".";
    }

    private String executeReminder(Payment payment) {
        String email = payment.getCustomerEmail() != null ? payment.getCustomerEmail() : "customer";
        payment.setStatus("REMINDER_SENT");
        return "Payment reminder sent to " + email + " for " + payment.getPaymentId() + ".";
    }

    private String executeEscalate(Payment payment) {
        payment.setStatus("ESCALATED");
        return "Payment " + payment.getPaymentId() + " escalated to support team for manual review.";
    }

    private String executeStop(Payment payment) {
        payment.setStatus("STOPPED");
        return "Recovery stopped for payment " + payment.getPaymentId() + ". No further actions will be taken.";
    }

    private RecoveryActionDTO mapToDTO(RecoveryAction entity) {
        RecoveryActionDTO dto = new RecoveryActionDTO();
        dto.setId(entity.getId());
        dto.setPaymentId(entity.getPaymentId());
        dto.setAction(entity.getAction());
        dto.setReason(entity.getReason());
        dto.setConfidence(entity.getConfidence());
        dto.setExecutionStatus(entity.getExecutionStatus());
        dto.setExecutionMessage(entity.getExecutionMessage());
        dto.setCreatedAt(entity.getCreatedAt());
        dto.setExecutedAt(entity.getExecutedAt());
        return dto;
    }
}
