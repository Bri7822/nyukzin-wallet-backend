package com.bright.wallet.controller;

import com.bright.wallet.dto.TransactionResponse;
import com.bright.wallet.dto.TransferRequest;
import com.bright.wallet.dto.WithdrawRequest;
import com.bright.wallet.model.User;
import com.bright.wallet.service.TransactionService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/transactions")
public class TransactionController {

    private final TransactionService transactionService;

    public TransactionController(TransactionService transactionService) {
        this.transactionService = transactionService;
    }

    @PostMapping("/transfer")
    public ResponseEntity<Object> transfer(
            @Valid @RequestBody TransferRequest request,
            @AuthenticationPrincipal User currentUser) {
        try {
            TransactionResponse result = transactionService.transfer(
                    request.getSenderWalletId(),
                    request.getReceiverWalletId(),
                    request.getAmount(),
                    currentUser);
            return ResponseEntity.ok(result);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", e.getMessage()));
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY)
                    .body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "An unexpected error occurred."));
        }
    }

    @PostMapping("/wallet/{walletId}/withdraw")
    public ResponseEntity<Object> withdraw(
            @PathVariable Long walletId,
            @Valid @RequestBody WithdrawRequest request,
            @AuthenticationPrincipal User currentUser) {
        try {
            return ResponseEntity.ok(
                    transactionService.withdraw(walletId, request.getAmount(), currentUser));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", e.getMessage()));
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY)
                    .body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "An unexpected error occurred."));
        }
    }

    @PostMapping("/wallet/{walletId}/topup")
    public ResponseEntity<Object> topUp(
            @PathVariable Long walletId,
            @RequestBody Map<String, BigDecimal> body,
            @AuthenticationPrincipal User currentUser) {
        BigDecimal amount = body.get("amount");
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", "Amount must be greater than zero."));
        }
        try {
            return ResponseEntity.ok(
                    transactionService.topUp(walletId, amount, currentUser));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/wallet/{walletId}/history")
    public ResponseEntity<Object> getWalletHistory(
            @PathVariable Long walletId,
            @AuthenticationPrincipal User currentUser) {
        try {
            List<TransactionResponse> history =
                    transactionService.getWalletHistory(walletId, currentUser);
            return ResponseEntity.ok(history);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/wallet/{walletId}/sent")
    public ResponseEntity<Object> getSentTransactions(
            @PathVariable Long walletId,
            @AuthenticationPrincipal User currentUser) {
        try {
            return ResponseEntity.ok(
                    transactionService.getSentTransactions(walletId, currentUser));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/wallet/{walletId}/received")
    public ResponseEntity<Object> getReceivedTransactions(
            @PathVariable Long walletId,
            @AuthenticationPrincipal User currentUser) {
        try {
            return ResponseEntity.ok(
                    transactionService.getReceivedTransactions(walletId, currentUser));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/wallet/{walletId}/balance")
    public ResponseEntity<Object> getBalance(
            @PathVariable Long walletId,
            @AuthenticationPrincipal User currentUser) {
        try {
            return ResponseEntity.ok(
                    transactionService.getWalletBalance(walletId, currentUser));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping
    public List<TransactionResponse> getAllTransactions() {
        return transactionService.getAllTransactions();
    }

    @GetMapping("/{id}")
    public ResponseEntity<Object> getTransaction(@PathVariable Long id) {
        try {
            return ResponseEntity.ok(transactionService.getTransactionById(id));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", e.getMessage()));
        }
    }
}