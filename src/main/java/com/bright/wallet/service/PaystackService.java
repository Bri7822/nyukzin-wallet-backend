package com.bright.wallet.service;

import com.bright.wallet.config.PaystackConfig;
import com.bright.wallet.dto.PaystackInitiateResponseDto;
import com.bright.wallet.dto.PaystackVerifyResponseDto;
import com.bright.wallet.model.Transaction;
import com.bright.wallet.model.Wallet;
import com.bright.wallet.repository.TransactionRepository;
import com.bright.wallet.repository.WalletRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.*;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.util.HexFormat;

/**
 * PaystackService
 *
 * Handles all business logic for Paystack-powered wallet deposits:
 *
 *  1. initiateDeposit()  — calls Paystack /transaction/initialize, returns redirect URL
 *  2. verifyAndCredit()  — calls Paystack /transaction/verify/{ref}, credits wallet
 *  3. handleWebhook()    — validates HMAC-SHA512 signature, processes charge.success
 *
 * Currency: ZAR.  Paystack expects amounts in KOBO / CENTS (smallest unit).
 *           1 ZAR = 100 cents  →  R100.00 = 10000 in Paystack API.
 *
 * Idempotency: paystackReference column has a UNIQUE constraint.
 *              A duplicate reference will throw DataIntegrityViolationException
 *              which we catch and swallow gracefully.
 */
@Service
public class PaystackService {

    private static final String PAYSTACK_INIT_PATH   = "/transaction/initialize";
    private static final String PAYSTACK_VERIFY_PATH = "/transaction/verify/";
    private static final String HMAC_ALGORITHM       = "HmacSHA512";
    private static final MediaType JSON_TYPE         =
            MediaType.get("application/json; charset=utf-8");

    private final PaystackConfig        config;
    private final OkHttpClient          httpClient;
    private final ObjectMapper          mapper;
    private final WalletRepository      walletRepository;
    private final TransactionRepository transactionRepository;

    public PaystackService(PaystackConfig config,
                           OkHttpClient httpClient,
                           ObjectMapper mapper,
                           WalletRepository walletRepository,
                           TransactionRepository transactionRepository) {
        this.config               = config;
        this.httpClient           = httpClient;
        this.mapper               = mapper;
        this.walletRepository     = walletRepository;
        this.transactionRepository = transactionRepository;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 1. INITIATE DEPOSIT
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Calls Paystack /transaction/initialize and returns the checkout URL.
     *
     * @param walletId  The wallet receiving the deposit (used as metadata)
     * @param amount    ZAR amount (e.g. BigDecimal("100.00") → R100.00)
     * @param email     User's email — Paystack displays it on the checkout page
     * @return PaystackInitiateResponseDto containing authorizationUrl + reference
     */
    public PaystackInitiateResponseDto initiateDeposit(Long walletId,
                                                       BigDecimal amount,
                                                       String email) {
        // Convert ZAR to cents (Paystack smallest unit for ZAR)
        long amountInCents = amount
                .multiply(BigDecimal.valueOf(100))
                .setScale(0, RoundingMode.HALF_UP)
                .longValue();

        String bodyJson = buildInitPayload(email, amountInCents, walletId);

        Request request = new Request.Builder()
                .url(config.getBaseUrl() + PAYSTACK_INIT_PATH)
                .post(RequestBody.create(bodyJson, JSON_TYPE))
                .addHeader("Authorization", "Bearer " + config.getSecretKey())
                .addHeader("Content-Type", "application/json")
                .build();

        try (Response response = httpClient.newCall(request).execute()) {
            String responseBody = response.body() != null ? response.body().string() : "";

            if (!response.isSuccessful()) {
                throw new RuntimeException(
                        "Paystack initialization failed [HTTP " + response.code() + "]: " + responseBody);
            }

            JsonNode root = mapper.readTree(responseBody);

            if (!root.path("status").asBoolean()) {
                throw new RuntimeException(
                        "Paystack returned status=false: " + root.path("message").asText());
            }

            JsonNode data            = root.path("data");
            String authorizationUrl  = data.path("authorization_url").asText();
            String reference         = data.path("reference").asText();

            return new PaystackInitiateResponseDto(authorizationUrl, reference);

        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("Failed to initialize Paystack transaction: " + e.getMessage(), e);
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 2. VERIFY AND CREDIT
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Verifies a Paystack transaction and credits the wallet if payment succeeded.
     *
     * Called by:
     *  - GET /payments/verify?reference=xxx  (user returns from Paystack checkout)
     *  - handleWebhook() internally          (server-side confirmation)
     *
     * Idempotent: if the reference was already processed the existing transaction
     * is returned without double-crediting.
     *
     * @param reference Paystack transaction reference
     * @return PaystackVerifyResponseDto with status, amount, and message
     */
    @Transactional
    public PaystackVerifyResponseDto verifyAndCredit(String reference) {

        // ── Idempotency check ──────────────────────────────────────────────
        // If we already have this reference in the DB, return the existing record
        // without making another API call or crediting the wallet twice.
        if (transactionRepository.existsByPaystackReference(reference)) {
            Transaction existing = transactionRepository.findByPaystackReference(reference)
                    .orElseThrow();
            return new PaystackVerifyResponseDto(
                    existing.getStatus(),
                    existing.getAmount(),
                    reference,
                    existing.getStatus().equals("SUCCESS")
                            ? "Deposit already processed successfully."
                            : "Transaction status: " + existing.getStatus()
            );
        }

        // ── Call Paystack verify endpoint ──────────────────────────────────
        Request request = new Request.Builder()
                .url(config.getBaseUrl() + PAYSTACK_VERIFY_PATH + reference)
                .get()
                .addHeader("Authorization", "Bearer " + config.getSecretKey())
                .build();

        try (Response response = httpClient.newCall(request).execute()) {
            String responseBody = response.body() != null ? response.body().string() : "";

            if (!response.isSuccessful()) {
                throw new RuntimeException(
                        "Paystack verify call failed [HTTP " + response.code() + "]: " + responseBody);
            }

            JsonNode root = mapper.readTree(responseBody);
            if (!root.path("status").asBoolean()) {
                throw new RuntimeException(
                        "Paystack verify returned status=false: " + root.path("message").asText());
            }

            JsonNode data              = root.path("data");
            String paystackStatus      = data.path("status").asText();         // "success" | "failed"
            long   amountInCents       = data.path("amount").asLong();
            String metaWalletIdStr     = data.path("metadata").path("walletId").asText("");

            // Convert cents → ZAR
            BigDecimal amountZar = BigDecimal.valueOf(amountInCents)
                    .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);

            if (!"success".equalsIgnoreCase(paystackStatus)) {
                // Payment was not successful — record a FAILED transaction
                saveTransaction(null, amountZar, reference, "FAILED");
                return new PaystackVerifyResponseDto(
                        "FAILED", amountZar, reference, "Payment was not completed.");
            }

            // ── Resolve wallet ─────────────────────────────────────────────
            Long walletId = metaWalletIdStr.isEmpty() ? null : Long.parseLong(metaWalletIdStr);
            if (walletId == null) {
                throw new RuntimeException("walletId missing from Paystack metadata for reference: " + reference);
            }

            Wallet wallet = walletRepository.findById(walletId)
                    .orElseThrow(() -> new RuntimeException("Wallet not found: " + walletId));

            // ── Credit the wallet ──────────────────────────────────────────
            wallet.setBalance(wallet.getBalance().add(amountZar));
            walletRepository.save(wallet);

            // ── Persist the transaction record ─────────────────────────────
            saveTransaction(wallet, amountZar, reference, "SUCCESS");

            return new PaystackVerifyResponseDto(
                    "SUCCESS", amountZar, reference, "Wallet funded successfully.");

        } catch (DataIntegrityViolationException e) {
            // Race condition: two threads verified the same reference simultaneously.
            // The second one hits the UNIQUE constraint — return gracefully.
            Transaction existing = transactionRepository.findByPaystackReference(reference).orElse(null);
            if (existing != null) {
                return new PaystackVerifyResponseDto(
                        existing.getStatus(), existing.getAmount(), reference,
                        "Deposit already processed.");
            }
            throw new RuntimeException("Duplicate reference conflict: " + reference);
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("Failed to verify Paystack payment: " + e.getMessage(), e);
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 3. HANDLE WEBHOOK
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Validates the Paystack webhook HMAC-SHA512 signature and processes
     * charge.success events.
     *
     * Paystack signs the raw request body with your secret key using HMAC-SHA512.
     * The computed hex digest must match the X-Paystack-Signature header exactly.
     *
     * @param payload   Raw request body exactly as received (do NOT parse first)
     * @param signature Value of the X-Paystack-Signature header
     * @throws SecurityException if the signature does not match
     */
    @Transactional
    public void handleWebhook(String payload, String signature) {
        // ── 1. Validate HMAC-SHA512 signature ─────────────────────────────
        if (!isValidSignature(payload, signature)) {
            throw new SecurityException("Invalid Paystack webhook signature");
        }

        // ── 2. Parse event ─────────────────────────────────────────────────
        try {
            JsonNode root      = mapper.readTree(payload);
            String   eventType = root.path("event").asText();

            if (!"charge.success".equals(eventType)) {
                // We only handle charge.success; other events are acknowledged but ignored.
                return;
            }

            JsonNode data      = root.path("data");
            String   reference = data.path("reference").asText();

            // ── 3. Process the successful charge ───────────────────────────
            // verifyAndCredit handles idempotency — safe to call from both
            // the webhook and the redirect-based verify endpoint.
            verifyAndCredit(reference);

        } catch (SecurityException e) {
            throw e;
        } catch (Exception e) {
            // Log but don't throw: Paystack retries on non-2xx.
            // We return 200 from the controller to stop retries for non-signature errors.
            System.err.println("[Paystack Webhook] Processing error: " + e.getMessage());
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // PRIVATE HELPERS
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Builds the JSON payload for Paystack /transaction/initialize.
     * walletId is stored in metadata so we can resolve it during verification.
     */
    private String buildInitPayload(String email, long amountInCents, Long walletId) {
        return """
                {
                  "email": "%s",
                  "amount": %d,
                  "currency": "ZAR",
                  "callback_url": "%s",
                  "metadata": {
                    "walletId": "%d",
                    "custom_fields": [
                      {
                        "display_name": "Wallet ID",
                        "variable_name": "wallet_id",
                        "value": "%d"
                      }
                    ]
                  }
                }
                """.formatted(email, amountInCents, config.getCallbackUrl(), walletId, walletId);
    }

    /**
     * Validates that HMAC-SHA512(secretKey, payload) == signature.
     */
    private boolean isValidSignature(String payload, String signature) {
        try {
            Mac mac = Mac.getInstance(HMAC_ALGORITHM);
            mac.init(new SecretKeySpec(
                    config.getSecretKey().getBytes(StandardCharsets.UTF_8), HMAC_ALGORITHM));
            byte[] hash = mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));
            String computed = HexFormat.of().formatHex(hash);
            return computed.equalsIgnoreCase(signature);
        } catch (Exception e) {
            System.err.println("[Paystack] Signature validation error: " + e.getMessage());
            return false;
        }
    }

    /**
     * Persists a DEPOSIT transaction to the database.
     *
     * @param wallet     The wallet being credited (null if payment failed)
     * @param amount     ZAR amount
     * @param reference  Paystack reference
     * @param status     "SUCCESS" | "FAILED"
     */
    private void saveTransaction(Wallet wallet, BigDecimal amount,
                                 String reference, String status) {
        Transaction tx = new Transaction();
        tx.setReceiverWallet(wallet);        // null for FAILED
        tx.setSenderWallet(null);            // DEPOSIT: no peer sender
        tx.setAmount(amount);
        tx.setType("DEPOSIT");
        tx.setStatus(status);
        tx.setProvider("PAYSTACK");
        tx.setPaystackReference(reference);
        transactionRepository.save(tx);
    }
}