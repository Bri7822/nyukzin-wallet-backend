package com.bright.wallet.service;

import com.bright.wallet.dto.TransactionResponse;
import com.bright.wallet.exception.UnauthorizedAccessException;
import com.bright.wallet.model.Transaction;
import com.bright.wallet.model.User;
import com.bright.wallet.model.Wallet;
import com.bright.wallet.repository.TransactionRepository;
import com.bright.wallet.repository.WalletRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class TransactionService {

    private final TransactionRepository transactionRepository;
    private final WalletRepository      walletRepository;
    private final EmailService          emailService;           // ← added

    public TransactionService(TransactionRepository transactionRepository,
                              WalletRepository walletRepository,
                              EmailService emailService) {      // ← added
        this.transactionRepository = transactionRepository;
        this.walletRepository      = walletRepository;
        this.emailService          = emailService;              // ← added
    }

    // ─── GUARDS ───────────────────────────────────────────────────────────────

    private void blockIfAdmin(User user) {
        if (user.getRole().equals("ROLE_ADMIN")) {
            throw new IllegalArgumentException(
                    "Admin accounts cannot perform transactions. " +
                            "Admin access is for management only.");
        }
    }

    private void blockIfReceiverIsAdmin(Wallet receiverWallet) {
        if (receiverWallet.getUser().getRole().equals("ROLE_ADMIN")) {
            throw new IllegalArgumentException(
                    "Cannot transfer money to an admin account. " +
                            "Admin accounts do not participate in transactions.");
        }
    }

    // ─── TRANSFER ─────────────────────────────────────────────────────────────

    @Transactional
    public TransactionResponse transfer(Long senderWalletId, Long receiverWalletId,
                                        BigDecimal amount, User currentUser) {
        blockIfAdmin(currentUser);

        Wallet senderWallet = walletRepository
                .findByIdAndUser(senderWalletId, currentUser)
                .orElseThrow(() -> new UnauthorizedAccessException(
                        "Access denied. Wallet " + senderWalletId + " does not belong to you."));

        Wallet receiverWallet = findWalletById(receiverWalletId);

        blockIfReceiverIsAdmin(receiverWallet);

        if (senderWalletId.equals(receiverWalletId)) {
            throw new IllegalArgumentException("Cannot transfer money to the same wallet.");
        }

        Transaction transaction = buildTransaction(senderWallet, receiverWallet, amount, "SEND");

        try {
            if (senderWallet.getBalance().compareTo(amount) < 0) {
                transaction.setStatus("FAILED");
                transactionRepository.save(transaction);
                throw new IllegalStateException(
                        "Insufficient funds. Available balance: " + senderWallet.getBalance());
            }

            senderWallet.setBalance(senderWallet.getBalance().subtract(amount));
            walletRepository.save(senderWallet);

            receiverWallet.setBalance(receiverWallet.getBalance().add(amount));
            walletRepository.save(receiverWallet);

            transaction.setStatus("SUCCESS");
            TransactionResponse response =
                    new TransactionResponse(transactionRepository.save(transaction));

            // ── Notify both parties ───────────────────────────────────────────
            String senderName    = senderWallet.getUser().getName();
            String senderEmail   = senderWallet.getUser().getEmail();
            String receiverName  = receiverWallet.getUser().getName();
            String receiverEmail = receiverWallet.getUser().getEmail();

            emailService.sendTransferSentEmail(
                    senderName, senderEmail,
                    receiverName,
                    amount,
                    senderWallet.getBalance()       // balance after deduction
            );

            emailService.sendTransferReceivedEmail(
                    receiverName, receiverEmail,
                    senderName,
                    amount,
                    receiverWallet.getBalance()     // balance after credit
            );
            // ─────────────────────────────────────────────────────────────────

            return response;

        } catch (IllegalStateException e) {
            throw e;
        } catch (Exception e) {
            transaction.setStatus("FAILED");
            transactionRepository.save(transaction);
            throw new RuntimeException("Transfer failed due to an unexpected error.", e);
        }
    }

    // ─── WITHDRAW ─────────────────────────────────────────────────────────────

    @Transactional
    public TransactionResponse withdraw(Long walletId, BigDecimal amount, User currentUser) {
        blockIfAdmin(currentUser);

        Wallet wallet = walletRepository
                .findByIdAndUser(walletId, currentUser)
                .orElseThrow(() -> new UnauthorizedAccessException(
                        "Access denied. Wallet " + walletId + " does not belong to you."));

        Transaction transaction = new Transaction();
        transaction.setSenderWallet(wallet);
        transaction.setReceiverWallet(null);
        transaction.setAmount(amount);
        transaction.setType("WITHDRAW");
        transaction.setStatus("PENDING");

        try {
            if (wallet.getBalance().compareTo(amount) < 0) {
                transaction.setStatus("FAILED");
                transactionRepository.save(transaction);
                throw new IllegalStateException(
                        "Insufficient funds. Available balance: " + wallet.getBalance());
            }

            wallet.setBalance(wallet.getBalance().subtract(amount));
            walletRepository.save(wallet);

            transaction.setStatus("SUCCESS");
            TransactionResponse response =
                    new TransactionResponse(transactionRepository.save(transaction));

            // ── Notify user ───────────────────────────────────────────────────
            emailService.sendWithdrawalEmail(
                    currentUser.getName(),
                    currentUser.getEmail(),
                    amount,
                    wallet.getBalance()     // balance after deduction
            );
            // ─────────────────────────────────────────────────────────────────

            return response;

        } catch (IllegalStateException e) {
            throw e;
        } catch (Exception e) {
            transaction.setStatus("FAILED");
            transactionRepository.save(transaction);
            throw new RuntimeException("Withdrawal failed due to an unexpected error.", e);
        }
    }

    // ─── TOP UP ───────────────────────────────────────────────────────────────

    @Transactional
    public Wallet topUp(Long walletId, BigDecimal amount, User currentUser) {
        blockIfAdmin(currentUser);

        Wallet wallet = walletRepository
                .findByIdAndUser(walletId, currentUser)
                .orElseThrow(() -> new UnauthorizedAccessException(
                        "Access denied. Wallet " + walletId + " does not belong to you."));

        wallet.setBalance(wallet.getBalance().add(amount));
        return walletRepository.save(wallet);
    }

    // ─── HISTORY & BALANCE ────────────────────────────────────────────────────

    public Map<String, Object> getWalletBalance(Long walletId, User currentUser) {
        Wallet wallet = walletRepository
                .findByIdAndUser(walletId, currentUser)
                .orElseThrow(() -> new UnauthorizedAccessException(
                        "Access denied. Wallet " + walletId + " does not belong to you."));
        return Map.of(
                "walletId", wallet.getId(),
                "owner",    wallet.getUser().getName(),
                "balance",  wallet.getBalance()
        );
    }

    public List<TransactionResponse> getWalletHistory(Long walletId, User currentUser) {
        Wallet wallet = walletRepository
                .findByIdAndUser(walletId, currentUser)
                .orElseThrow(() -> new UnauthorizedAccessException(
                        "Access denied. Wallet " + walletId + " does not belong to you."));

        return transactionRepository
                .findBySenderWalletIdOrReceiverWalletIdOrderByCreatedAtDesc(
                        wallet.getId(), wallet.getId())
                .stream()
                .map(TransactionResponse::new)
                .collect(Collectors.toList());
    }

    public List<TransactionResponse> getSentTransactions(Long walletId, User currentUser) {
        Wallet wallet = walletRepository
                .findByIdAndUser(walletId, currentUser)
                .orElseThrow(() -> new UnauthorizedAccessException(
                        "Access denied. Wallet " + walletId + " does not belong to you."));

        return transactionRepository
                .findBySenderWalletId(wallet.getId())
                .stream()
                .map(TransactionResponse::new)
                .collect(Collectors.toList());
    }

    public List<TransactionResponse> getReceivedTransactions(Long walletId, User currentUser) {
        Wallet wallet = walletRepository
                .findByIdAndUser(walletId, currentUser)
                .orElseThrow(() -> new UnauthorizedAccessException(
                        "Access denied. Wallet " + walletId + " does not belong to you."));

        return transactionRepository
                .findByReceiverWalletId(wallet.getId())
                .stream()
                .map(TransactionResponse::new)
                .collect(Collectors.toList());
    }

    public List<TransactionResponse> getAllTransactions() {
        return transactionRepository.findAll()
                .stream().map(TransactionResponse::new).collect(Collectors.toList());
    }

    public TransactionResponse getTransactionById(Long id) {
        return new TransactionResponse(
                transactionRepository.findById(id)
                        .orElseThrow(() -> new IllegalArgumentException(
                                "Transaction not found: " + id)));
    }

    // ─── PRIVATE HELPERS ──────────────────────────────────────────────────────

    private Wallet findWalletById(Long walletId) {
        return walletRepository.findById(walletId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Wallet not found: " + walletId));
    }

    private Transaction buildTransaction(Wallet sender, Wallet receiver,
                                         BigDecimal amount, String type) {
        Transaction tx = new Transaction();
        tx.setSenderWallet(sender);
        tx.setReceiverWallet(receiver);
        tx.setAmount(amount);
        tx.setType(type);
        tx.setStatus("PENDING");
        return tx;
    }
}