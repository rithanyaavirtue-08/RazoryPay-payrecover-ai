package com.payrecover.repository;

import com.payrecover.entity.RecoveryAction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RecoveryActionRepository extends JpaRepository<RecoveryAction, Long> {

    List<RecoveryAction> findByPaymentIdOrderByCreatedAtDesc(String paymentId);

    Optional<RecoveryAction> findFirstByPaymentIdAndExecutionStatusOrderByCreatedAtDesc(
            String paymentId, String executionStatus);

    List<RecoveryAction> findAllByOrderByCreatedAtDesc();
}
