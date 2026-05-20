package com.bright.wallet.repository;

import com.bright.wallet.model.User;
import com.bright.wallet.model.Wallet;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface WalletRepository extends JpaRepository<Wallet, Long> {

    // Find wallet by its linked user
    Optional<Wallet> findByUser(User user);

    // Find wallet by ID AND user together
    // This is the ownership check — if wallet ID exists but belongs to someone
    // else, this returns empty and we throw 403
    // SELECT * FROM wallets WHERE id = ? AND user_id = ?
    Optional<Wallet> findByIdAndUser(Long id, User user);
}