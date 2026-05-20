package com.bright.wallet.controller;

import com.bright.wallet.dto.PaystackDepositRequest;
import com.bright.wallet.dto.PaystackInitiateResponseDto;
import com.bright.wallet.dto.PaystackVerifyResponseDto;
import com.bright.wallet.model.User;
import com.bright.wallet.model.Wallet;
import com.bright.wallet.repository.WalletRepository;
import com.bright.wallet.service.PaystackService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/payments")
public class PaystackController {

    private final PaystackService  paystackService;
    private final WalletRepository walletRepository;

    public PaystackController(PaystackService paystackService,
                              WalletRepository walletRepository) {
        this.paystackService  = paystackService;
        this.walletRepository = walletRepository;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // POST /payments/deposit/initiate
    // ─────────────────────────────────────────────────────────────────────────

    @PostMapping("/deposit/initiate")
    public ResponseEntity<?> initiateDeposit(
            @Valid @RequestBody PaystackDepositRequest request,
            @AuthenticationPrincipal User currentUser) {

        try {
            Wallet wallet = walletRepository.findByUser(currentUser)
                    .orElseThrow(() -> new RuntimeException("Wallet not found"));

            PaystackInitiateResponseDto response = paystackService.initiateDeposit(
                    wallet.getId(),
                    request.getAmount(),
                    currentUser.getEmail()
            );

            return ResponseEntity.ok(response);

        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
                    .body(Map.of("error", "Failed to initiate Paystack payment: " + e.getMessage()));
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // GET /payments/verify?reference={ref}
    // ─────────────────────────────────────────────────────────────────────────

    @GetMapping("/verify")
    public ResponseEntity<?> verifyPayment(
            @RequestParam("reference") String reference,
            @AuthenticationPrincipal User currentUser) {

        if (reference == null || reference.isBlank()) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Payment reference is required."));
        }

        try {
            PaystackVerifyResponseDto result = paystackService.verifyAndCredit(reference);
            return ResponseEntity.ok(result);

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
                    .body(Map.of("error", "Verification failed: " + e.getMessage()));
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // POST /payments/webhook
    // ─────────────────────────────────────────────────────────────────────────

    @PostMapping("/webhook")
    public ResponseEntity<String> handleWebhook(
            @RequestBody byte[] payload,
            @RequestHeader(value = "X-Paystack-Signature", required = false) String signature) {

        if (signature == null || signature.isBlank()) {
            System.err.println("[Paystack] Webhook received without X-Paystack-Signature header");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Missing signature");
        }

        try {
            String payloadStr = new String(payload, java.nio.charset.StandardCharsets.UTF_8);
            paystackService.handleWebhook(payloadStr, signature);
            return ResponseEntity.ok("Received");

        } catch (SecurityException e) {
            System.err.println("[Paystack] Invalid webhook signature: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid signature");

        } catch (Exception e) {
            System.err.println("[Paystack] Webhook processing error: " + e.getMessage());
            return ResponseEntity.ok("Error logged");
        }
    }
}