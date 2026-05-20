package com.bright.wallet.dto;

import com.bright.wallet.model.Transaction;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Safe transaction response — controls exactly what the client sees.
 *
 * Before: raw Transaction entity was returned which included
 * full Wallet → full User → password, role, email of BOTH parties.
 *
 * Now: only safe, necessary fields are exposed.
 * - No passwords
 * - No roles
 * - No emails of other users
 * - Just names and wallet IDs
 */
public class TransactionResponse {

    private Long transactionId;

    // Sender info — only what's needed
    private Long senderWalletId;
    private String senderName;

    // Receiver info — only name and wallet ID
    // receiverWallet can be null for WITHDRAW transactions
    private Long receiverWalletId;
    private String receiverName;

    private BigDecimal amount;
    private String type;    // SEND / WITHDRAW
    private String status;  // SUCCESS / FAILED / PENDING
    private LocalDateTime createdAt;

    public TransactionResponse(Transaction tx) {
        this.transactionId    = tx.getId();
        this.amount           = tx.getAmount();
        this.type             = tx.getType();
        this.status           = tx.getStatus();
        this.createdAt        = tx.getCreatedAt();

        // Sender wallet — always present
        if (tx.getSenderWallet() != null) {
            this.senderWalletId = tx.getSenderWallet().getId();
            this.senderName     = tx.getSenderWallet().getUser().getName();
        }

        // Receiver wallet — null for WITHDRAW transactions (money leaves system)
        if (tx.getReceiverWallet() != null) {
            this.receiverWalletId = tx.getReceiverWallet().getId();
            this.receiverName     = tx.getReceiverWallet().getUser().getName();
        }
    }

    public Long getTransactionId()     { return transactionId; }
    public Long getSenderWalletId()    { return senderWalletId; }
    public String getSenderName()      { return senderName; }
    public Long getReceiverWalletId()  { return receiverWalletId; }
    public String getReceiverName()    { return receiverName; }
    public BigDecimal getAmount()      { return amount; }
    public String getType()            { return type; }
    public String getStatus()          { return status; }
    public LocalDateTime getCreatedAt(){ return createdAt; }
}