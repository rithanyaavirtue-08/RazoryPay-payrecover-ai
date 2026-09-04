package com.payrecover.repository;

import com.payrecover.entity.Payment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, Long> {

    @Query("SELECT SUM(p.amount) FROM Payment p WHERE p.status = :status")
    BigDecimal sumAmountByStatus(String status);

    long countByStatus(String status);

    long countByStatusAndAttemptCountLessThan(String status, Integer attemptCount);
}
