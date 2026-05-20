package com.bright.wallet.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "transactions")
public class Transaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // null for DEPOSIT — funds come from a payment provider, not a peer wallet
    @ManyToOne
    @JoinColumn(name = "sender_wallet_id")
    private Wallet senderWallet;

    @ManyToOne
    @JoinColumn(name = "receiver_wallet_id")
    private Wallet receiverWallet;

    private BigDecimal amount;

    // SEND | WITHDRAW | DEPOSIT
    private String type;

    // SUCCESS | FAILED | PENDING
    private String status;

    private LocalDateTime createdAt = LocalDateTime.now();

    // ── Payment Provider ──────────────────────────────────────────────────────
    // Identifies which payment rail was used for DEPOSIT transactions.
    // Values: "PAYSTACK" | "STRIPE" | "INTERNAL"
    // null for peer-to-peer SEND / WITHDRAW transactions.
    @Column(name = "provider")
    private String provider;


    // ── Paystack ──────────────────────────────────────────────────────────────
    // Paystack transaction reference — alphanumeric string Paystack generates.
    // unique = true: prevents double-crediting if webhook fires more than once.
    // Also used to call /transaction/verify/{reference} for manual verification.
    @Column(name = "paystack_reference", unique = true)
    private String paystackReference;

    public Transaction() {}

    // ── Getters & Setters ──────────────────────────────────────────────────────

    public Long getId()                              { return id; }

    public Wallet getSenderWallet()                  { return senderWallet; }
    public void   setSenderWallet(Wallet w)          { this.senderWallet = w; }

    public Wallet getReceiverWallet()                { return receiverWallet; }
    public void   setReceiverWallet(Wallet w)        { this.receiverWallet = w; }

    public BigDecimal getAmount()                    { return amount; }
    public void       setAmount(BigDecimal amount)   { this.amount = amount; }

    public String getType()                          { return type; }
    public void   setType(String type)               { this.type = type; }

    public String getStatus()                        { return status; }
    public void   setStatus(String status)           { this.status = status; }

    public LocalDateTime getCreatedAt()              { return createdAt; }

    // Getters & setters
    public String getPaystackReference() { return paystackReference; }
    public void setPaystackReference(String paystackReference) { this.paystackReference = paystackReference; }

    public String getProvider() { return provider; }
    public void setProvider(String provider) { this.provider = provider; }
}