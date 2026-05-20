package com.bright.wallet.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

/**
 * PaystackDepositRequest
 *
 * Body sent by the frontend to POST /payments/deposit/initiate.
 * The amount is in ZAR (e.g. 100.00 = R100).
 */
public class PaystackDepositRequest {

    @NotNull(message = "Amount is required")
    @DecimalMin(value = "5.00", message = "Minimum deposit is R5.00")
    private BigDecimal amount;

    public PaystackDepositRequest() {}

    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
}