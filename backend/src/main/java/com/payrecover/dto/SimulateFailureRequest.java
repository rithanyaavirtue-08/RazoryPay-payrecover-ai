package com.payrecover.dto;

public class SimulateFailureRequest {

    private String failureReason = "payment_failed";

    public String getFailureReason() { return failureReason; }
    public void setFailureReason(String failureReason) { this.failureReason = failureReason; }
}
