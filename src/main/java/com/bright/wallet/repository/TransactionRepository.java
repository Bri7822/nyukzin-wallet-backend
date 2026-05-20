package com.bright.wallet.repository;

import com.bright.wallet.model.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface TransactionRepository extends JpaRepository<Transaction, Long> {

    // ── Wallet history (ID-based, not object-based) ───────────────────────────
    List<Transaction> findBySenderWalletId(Long walletId);
    List<Transaction> findByReceiverWalletId(Long walletId);

    // ordered variant used by getWalletHistory()
    List<Transaction> findBySenderWalletIdOrReceiverWalletIdOrderByCreatedAtDesc(
            Long senderWalletId, Long receiverWalletId);

    // ── Paystack idempotency ──────────────────────────────────────────────────
    boolean existsByPaystackReference(String paystackReference);
    Optional<Transaction> findByPaystackReference(String paystackReference);

    void deleteAllBySenderWalletIdOrReceiverWalletId(Long senderWalletId, Long receiverWalletId);
}