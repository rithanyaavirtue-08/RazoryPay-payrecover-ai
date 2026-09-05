package com.payrecover.config;

import com.payrecover.entity.Payment;
import com.payrecover.repository.PaymentRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import java.math.BigDecimal;

@Configuration
@Profile({"local", "docker"})
public class DataInitializer {

    @Bean
    CommandLineRunner seedPayments(PaymentRepository paymentRepository) {
        return args -> {
            if (paymentRepository.count() > 0) {
                return;
            }

            Payment pay1 = new Payment();
            pay1.setPaymentId("pay_1");
            pay1.setAmount(new BigDecimal("500.00"));
            pay1.setCurrency("INR");
            pay1.setStatus("FAILED");
            pay1.setFailureReason("insufficient_funds");
            pay1.setCustomerEmail("customer1@example.com");
            pay1.setAttemptCount(2);
            paymentRepository.save(pay1);

            Payment pay2 = new Payment();
            pay2.setPaymentId("pay_2");
            pay2.setAmount(new BigDecimal("1200.50"));
            pay2.setCurrency("INR");
            pay2.setStatus("FAILED");
            pay2.setFailureReason("card_declined");
            pay2.setCustomerEmail("customer2@example.com");
            pay2.setAttemptCount(1);
            paymentRepository.save(pay2);
        };
    }
}
