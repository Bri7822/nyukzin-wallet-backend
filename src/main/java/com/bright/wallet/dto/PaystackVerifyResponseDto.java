package com.bright.wallet.dto;

import java.math.BigDecimal;

/**
 * PaystackVerifyResponseDto
 *
 * Returned by GET /payments/verify?reference={ref}.
 * Gives the frontend all the information it needs to show a receipt.
 */
public class PaystackVerifyResponseDto {

    /** Transaction status as recorded in our database: SUCCESS | FAILED | PENDING */
    private String status;

    /** ZAR amount credited to the wallet. */
    private BigDecimal amount;

    /** Paystack reference used for this transaction. */
    private String reference;

    /** Human-readable message for UI display. */
    private String message;

    public PaystackVerifyResponseDto() {}

    public PaystackVerifyResponseDto(String status, BigDecimal amount,
                                     String reference, String message) {
        this.status    = status;
        this.amount    = amount;
        this.reference = reference;
        this.message   = message;
    }

    public String     getStatus()    { return status; }
    public BigDecimal getAmount()    { return amount; }
    public String     getReference() { return reference; }
    public String     getMessage()   { return message; }
}