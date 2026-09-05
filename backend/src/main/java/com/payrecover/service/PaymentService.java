package com.payrecover.service;

import com.payrecover.dto.PaymentDTO;
import com.payrecover.dto.RevenueAtRiskDTO;
import com.payrecover.entity.Payment;
import com.payrecover.repository.PaymentRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class PaymentService {

    private final PaymentRepository paymentRepository;

    public PaymentService(PaymentRepository paymentRepository) {
        this.paymentRepository = paymentRepository;
    }

    public PaymentDTO createPayment(PaymentDTO dto) {
        Payment payment = new Payment();
        mapToEntity(dto, payment);
        
        Payment saved = paymentRepository.save(payment);
        return mapToDTO(saved);
    }

    public List<PaymentDTO> getAllPayments() {
        return paymentRepository.findAll().stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    public PaymentDTO getPaymentById(Long id) {
        Payment payment = paymentRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Payment not found"));
        return mapToDTO(payment);
    }

    public RevenueAtRiskDTO calculateRevenueAtRisk() {
        String status = "FAILED";
        
        BigDecimal totalAtRiskAmount = paymentRepository.sumAmountByStatus(status);
        if (totalAtRiskAmount == null) {
            totalAtRiskAmount = BigDecimal.ZERO;
        }
        
        long failedPaymentCount = paymentRepository.countByStatus(status);
        long eligiblePaymentCount = paymentRepository.countByStatusAndAttemptCountLessThan(status, 3);
        
        RevenueAtRiskDTO dto = new RevenueAtRiskDTO();
        dto.setTotalAtRiskAmount(totalAtRiskAmount);
        dto.setFailedPaymentCount(failedPaymentCount);
        dto.setEligiblePaymentCount(eligiblePaymentCount);
        
        return dto;
    }

    private void mapToEntity(PaymentDTO dto, Payment entity) {
        entity.setPaymentId(dto.getPaymentId());
        entity.setAmount(dto.getAmount());
        entity.setCurrency(dto.getCurrency());
        entity.setStatus(dto.getStatus());
        entity.setFailureReason(dto.getFailureReason());
        entity.setCustomerEmail(dto.getCustomerEmail());
        if (dto.getAttemptCount() != null) {
            entity.setAttemptCount(dto.getAttemptCount());
        }
        if (dto.getRazorpayOrderId() != null) {
            entity.setRazorpayOrderId(dto.getRazorpayOrderId());
        }
        if (dto.getRazorpayPaymentId() != null) {
            entity.setRazorpayPaymentId(dto.getRazorpayPaymentId());
        }
        if (dto.getSource() != null) {
            entity.setSource(dto.getSource());
        } else if (entity.getSource() == null) {
            entity.setSource("MANUAL");
        }
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
