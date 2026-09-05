package com.payrecover.dto;

import java.math.BigDecimal;

public class RazorpayOrderRequest {

    private BigDecimal amount;
    private String currency = "INR";
    private String customerEmail;

    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }

    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }

    public String getCustomerEmail() { return customerEmail; }
    public void setCustomerEmail(String customerEmail) { this.customerEmail = customerEmail; }
}
