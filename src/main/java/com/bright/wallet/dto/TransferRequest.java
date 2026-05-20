package com.bright.wallet.dto;

import java.math.BigDecimal;

/**
 * Data Transfer Object (DTO) for the /transfer endpoint.
 *
 * We keep this separate from the Transaction model so the controller
 * only receives exactly what it needs — no internal fields exposed.
 *
 * Example JSON body:
 * {
 *   "senderWalletId": 1,
 *   "receiverWalletId": 2,
 *   "amount": 250.00
 * }
 */
public class TransferRequest {

    private Long senderWalletId;
    private Long receiverWalletId;
    private BigDecimal amount;

    // Default constructor (required for JSON deserialization)
    public TransferRequest() {}

    public TransferRequest(Long senderWalletId, Long receiverWalletId, BigDecimal amount) {
        this.senderWalletId = senderWalletId;
        this.receiverWalletId = receiverWalletId;
        this.amount = amount;
    }

    // Getters & Setters
    public Long getSenderWalletId() { return senderWalletId; }
    public void setSenderWalletId(Long senderWalletId) { this.senderWalletId = senderWalletId; }

    public Long getReceiverWalletId() { return receiverWalletId; }
    public void setReceiverWalletId(Long receiverWalletId) { this.receiverWalletId = receiverWalletId; }

    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
}