package com.payrecover.dto;

import java.math.BigDecimal;

public class RevenueAtRiskDTO {
    private BigDecimal totalAtRiskAmount;
    private long failedPaymentCount;
    private long eligiblePaymentCount;

    public RevenueAtRiskDTO() {
    }

    public BigDecimal getTotalAtRiskAmount() { return totalAtRiskAmount; }
    public void setTotalAtRiskAmount(BigDecimal totalAtRiskAmount) { this.totalAtRiskAmount = totalAtRiskAmount; }

    public long getFailedPaymentCount() { return failedPaymentCount; }
    public void setFailedPaymentCount(long failedPaymentCount) { this.failedPaymentCount = failedPaymentCount; }

    public long getEligiblePaymentCount() { return eligiblePaymentCount; }
    public void setEligiblePaymentCount(long eligiblePaymentCount) { this.eligiblePaymentCount = eligiblePaymentCount; }
}
