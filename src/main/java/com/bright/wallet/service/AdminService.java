package com.bright.wallet.service;

import com.bright.wallet.dto.TransactionResponse;
import com.bright.wallet.model.Transaction;
import com.bright.wallet.model.User;
import com.bright.wallet.model.Wallet;
import com.bright.wallet.repository.TransactionRepository;
import com.bright.wallet.repository.UserRepository;
import com.bright.wallet.repository.WalletRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * AdminService handles all admin-only operations.
 * These methods are only reachable through AdminController
 * which is protected by ROLE_ADMIN in SecurityConfig.
 *
 * Lives in: com.bright.wallet.service
 */
@Service
public class AdminService {

    private final UserRepository        userRepository;
    private final WalletRepository      walletRepository;
    private final TransactionRepository transactionRepository;

    public AdminService(UserRepository userRepository,
                        WalletRepository walletRepository,
                        TransactionRepository transactionRepository) {
        this.userRepository        = userRepository;
        this.walletRepository      = walletRepository;
        this.transactionRepository = transactionRepository;
    }

    // ─── USERS ────────────────────────────────────────────────────────────────

    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    public User getUserById(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + id));
    }

    /**
     * Delete a user and all their associated data.
     *
     * Order matters — must follow foreign key dependencies:
     *
     * 1. Find the user's wallet (if they have one)
     * 2. Delete all transactions referencing that wallet (sender OR receiver)
     *    → Without this step PostgreSQL throws a FK constraint violation (500)
     *    → Because transactions.sender_wallet_id / receiver_wallet_id
     *      still point to the wallet we're about to delete
     * 3. Delete the wallet
     * 4. Delete the user
     *
     * @Transactional wraps all 4 steps atomically —
     * if any step fails, everything rolls back and no partial data is left.
     */
    @Transactional
    public String deleteUser(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + id));

        // Step 1: find their wallet (admin accounts may not have one)
        Optional<Wallet> walletOpt = walletRepository.findByUser(user);

        if (walletOpt.isPresent()) {
            Wallet wallet = walletOpt.get();

            // Step 2: delete all transactions referencing this wallet
            // This removes both sent AND received transactions
            transactionRepository.deleteAllBySenderWalletIdOrReceiverWalletId(
                    wallet.getId(), wallet.getId());

            // Step 3: delete the wallet itself
            walletRepository.delete(wallet);
        }

        // Step 4: delete the user
        userRepository.delete(user);

        return "User '" + user.getName() + "' and all associated data deleted successfully";
    }

    public User promoteToAdmin(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));
        if (user.getRole().equals("ROLE_ADMIN")) {
            throw new IllegalArgumentException("User is already an admin.");
        }
        user.setRole("ROLE_ADMIN");
        return userRepository.save(user);
    }

    public User demoteToUser(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));
        if (user.getRole().equals("ROLE_USER")) {
            throw new IllegalArgumentException("User is already a regular user.");
        }
        user.setRole("ROLE_USER");
        return userRepository.save(user);
    }

    // ─── WALLETS ──────────────────────────────────────────────────────────────

    public List<Wallet> getAllWallets() {
        return walletRepository.findAll();
    }

    public Wallet getWalletById(Long id) {
        return walletRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Wallet not found: " + id));
    }

    // ─── TRANSACTIONS ─────────────────────────────────────────────────────────

    public List<TransactionResponse> getAllTransactions() {
        return transactionRepository.findAll()
                .stream()
                .map(TransactionResponse::new)
                .collect(Collectors.toList());
    }

    public TransactionResponse getTransactionById(Long id) {
        Transaction tx = transactionRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Transaction not found: " + id));
        return new TransactionResponse(tx);
    }
}