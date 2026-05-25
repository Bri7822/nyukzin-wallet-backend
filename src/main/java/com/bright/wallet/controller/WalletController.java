package com.bright.wallet.controller;

import com.bright.wallet.model.User;
import com.bright.wallet.model.Wallet;
import com.bright.wallet.repository.UserRepository;
import com.bright.wallet.repository.WalletRepository;
import com.bright.wallet.service.TransactionService;
import com.bright.wallet.dto.TransactionResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.Map;
import java.util.List;

@RestController
@RequestMapping("/wallets")
public class WalletController {

    private final WalletRepository     walletRepository;
    private final UserRepository       userRepository;
    private final TransactionService   transactionService;

    public WalletController(WalletRepository walletRepository,
                            UserRepository userRepository,
                            TransactionService transactionService) {
        this.walletRepository   = walletRepository;
        this.userRepository     = userRepository;
        this.transactionService = transactionService;
    }

    // ── GET /wallets/me — current user's wallet info ──────────────────────
    @GetMapping("/me")
    public ResponseEntity<?> getMyWallet(@AuthenticationPrincipal User currentUser) {
        return walletRepository.findByUser(currentUser)
                .map(w -> ResponseEntity.ok(Map.of(
                        "walletId", w.getId(),
                        "balance",  w.getBalance(),
                        "owner",    currentUser.getName(),
                        "email",    currentUser.getEmail()
                )))
                .orElse(ResponseEntity.notFound().build());
    }

    // ── GET /wallets/lookup?email=xxx — resolve email → wallet info ───────
    @GetMapping("/lookup")
    public ResponseEntity<?> lookupByEmail(@RequestParam String email,
                                           @AuthenticationPrincipal User currentUser) {
        // Prevent looking up yourself
        if (currentUser.getEmail().equalsIgnoreCase(email)) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "You cannot transfer money to yourself."));
        }

        User target = userRepository.findByEmail(email).orElse(null);
        if (target == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", "No user found with that email address."));
        }

        // Block transfers to admin accounts
        if (target.getRole().equals("ROLE_ADMIN")) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Cannot transfer money to an admin account."));
        }

        Wallet wallet = walletRepository.findByUser(target).orElse(null);
        if (wallet == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", "Wallet not found for that user."));
        }

        return ResponseEntity.ok(Map.of(
                "walletId", wallet.getId(),
                "name",     target.getName(),
                "email",    target.getEmail()
        ));
    }

    // ── GET /wallets/users — list all non-admin users for dropdown ─────────
    @GetMapping("/users")
    @org.springframework.transaction.annotation.Transactional(readOnly = true)
    public ResponseEntity<?> getAllUsers(@AuthenticationPrincipal User currentUser) {
        try {
            List<Map<String, Object>> users = userRepository.findAll()
                    .stream()
                    .filter(u -> !u.getRole().equals("ROLE_ADMIN"))
                    .filter(u -> !u.getEmail().equals(currentUser.getEmail()))
                    .map(u -> Map.<String, Object>of(
                            "name",  u.getName(),
                            "email", u.getEmail()
                    ))
                    .toList();
            return ResponseEntity.ok(users);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    // ── GET /wallets/{id}/balance ─────────────────────────────────────────
    @GetMapping("/{id}/balance")
    public ResponseEntity<?> getBalance(@PathVariable Long id,
                                        @AuthenticationPrincipal User currentUser) {
        try {
            return ResponseEntity.ok(transactionService.getWalletBalance(id, currentUser));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    // ── GET /wallets/{id}/history ─────────────────────────────────────────
    @GetMapping("/{id}/history")
    public ResponseEntity<?> getHistory(@PathVariable Long id,
                                        @AuthenticationPrincipal User currentUser) {
        try {
            return ResponseEntity.ok(transactionService.getWalletHistory(id, currentUser));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    // ── POST /wallets/{id}/transfer ───────────────────────────────────────
    @PostMapping("/{id}/transfer")
    public ResponseEntity<?> transfer(@PathVariable Long id,
                                      @RequestBody Map<String, Object> body,
                                      @AuthenticationPrincipal User currentUser) {
        try {
            Long receiverWalletId = Long.valueOf(body.get("receiverWalletId").toString());
            BigDecimal amount     = new BigDecimal(body.get("amount").toString());
            TransactionResponse result = transactionService.transfer(id, receiverWalletId, amount, currentUser);
            return ResponseEntity.ok(result);
        } catch (IllegalStateException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Transfer failed. Please try again."));
        }
    }

    // ── POST /wallets/{id}/withdraw ───────────────────────────────────────
    @PostMapping("/{id}/withdraw")
    public ResponseEntity<?> withdraw(@PathVariable Long id,
                                      @RequestBody Map<String, Object> body,
                                      @AuthenticationPrincipal User currentUser) {
        try {
            BigDecimal amount = new BigDecimal(body.get("amount").toString());
            TransactionResponse result = transactionService.withdraw(id, amount, currentUser);
            return ResponseEntity.ok(result);
        } catch (IllegalStateException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Withdrawal failed. Please try again."));
        }
    }
}