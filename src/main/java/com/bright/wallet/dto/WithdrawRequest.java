package com.bright.wallet.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

/**
 * DTO for the withdrawal endpoint.
 *
 * Example JSON body:
 * {
 *   "amount": 200.00
 * }
 *
 * The walletId comes from the URL path — not the body.
 * So the body only needs the amount.
 */
public class WithdrawRequest {

    @NotNull(message = "Amount is required")
    @DecimalMin(value = "0.01", message = "Withdrawal amount must be at least 0.01")
    private BigDecimal amount;

    // Default constructor (required for JSON deserialization)
    public WithdrawRequest() {}

    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
}