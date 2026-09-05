package com.payrecover.service;

import com.payrecover.entity.Payment;
import com.payrecover.entity.RecoveryAction;
import com.payrecover.repository.PaymentRepository;
import com.payrecover.repository.RecoveryActionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RecoveryExecutionServiceTest {

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private RecoveryActionRepository recoveryActionRepository;

    @Mock
    private RazorpayService razorpayService;

    private RecoveryExecutionService recoveryExecutionService;

    @BeforeEach
    void setUp() {
        recoveryExecutionService = new RecoveryExecutionService(
                paymentRepository,
                recoveryActionRepository,
                razorpayService
        );
    }

    @Test
    void executeRetryMarksPaymentPendingForManualPayments() {
        Payment payment = buildPayment("MANUAL");
        RecoveryAction action = buildRecommendedAction("RETRY");

        when(paymentRepository.findByPaymentId("pay_1")).thenReturn(Optional.of(payment));
        when(recoveryActionRepository.findFirstByPaymentIdAndExecutionStatusOrderByCreatedAtDesc(
                "pay_1", "RECOMMENDED")).thenReturn(Optional.of(action));
        when(recoveryActionRepository.save(any(RecoveryAction.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(paymentRepository.save(any(Payment.class))).thenAnswer(invocation -> invocation.getArgument(0));

        recoveryExecutionService.executeLatestRecommendation("pay_1");

        ArgumentCaptor<Payment> paymentCaptor = ArgumentCaptor.forClass(Payment.class);
        verify(paymentRepository).save(paymentCaptor.capture());
        assertEquals("PENDING", paymentCaptor.getValue().getStatus());
        assertEquals(2, paymentCaptor.getValue().getAttemptCount());
    }

    @Test
    void executeStopMarksPaymentStopped() {
        Payment payment = buildPayment("MANUAL");
        RecoveryAction action = buildRecommendedAction("STOP");

        when(paymentRepository.findByPaymentId("pay_1")).thenReturn(Optional.of(payment));
        when(recoveryActionRepository.findFirstByPaymentIdAndExecutionStatusOrderByCreatedAtDesc(
                "pay_1", "RECOMMENDED")).thenReturn(Optional.of(action));
        when(recoveryActionRepository.save(any(RecoveryAction.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(paymentRepository.save(any(Payment.class))).thenAnswer(invocation -> invocation.getArgument(0));

        recoveryExecutionService.executeLatestRecommendation("pay_1");

        ArgumentCaptor<Payment> paymentCaptor = ArgumentCaptor.forClass(Payment.class);
        verify(paymentRepository).save(paymentCaptor.capture());
        assertEquals("STOPPED", paymentCaptor.getValue().getStatus());
    }

    private Payment buildPayment(String source) {
        Payment payment = new Payment();
        payment.setPaymentId("pay_1");
        payment.setAmount(new BigDecimal("500.00"));
        payment.setCurrency("INR");
        payment.setStatus("FAILED");
        payment.setAttemptCount(1);
        payment.setSource(source);
        return payment;
    }

    private RecoveryAction buildRecommendedAction(String actionType) {
        RecoveryAction action = new RecoveryAction();
        action.setPaymentId("pay_1");
        action.setAction(actionType);
        action.setReason("Test reason");
        action.setConfidence(0.9);
        action.setExecutionStatus("RECOMMENDED");
        return action;
    }
}
