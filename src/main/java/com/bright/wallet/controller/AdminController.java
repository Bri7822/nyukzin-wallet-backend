package com.bright.wallet.controller;

import com.bright.wallet.dto.TransactionResponse;
import com.bright.wallet.model.User;
import com.bright.wallet.model.Wallet;
import com.bright.wallet.service.AdminService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * AdminController — all endpoints start with /admin
 *
 * SecurityConfig protects ALL of these with .hasRole("ADMIN")
 * Any request without ROLE_ADMIN gets 403 automatically —
 * we don't need to check the role inside these methods.
 *
 * Lives in: com.bright.wallet.controller
 */
@RestController
@RequestMapping("/admin")
public class AdminController {

    private final AdminService adminService;

    public AdminController(AdminService adminService) {
        this.adminService = adminService;
    }

    // ─── USER ENDPOINTS ───────────────────────────────────────────────────────

    @GetMapping("/users")
    public List<User> getAllUsers() {
        return adminService.getAllUsers();
    }

    @GetMapping("/users/{id}")
    public ResponseEntity<Object> getUserById(@PathVariable Long id) {
        return ResponseEntity.ok(adminService.getUserById(id));
    }

    @DeleteMapping("/users/{id}")
    public ResponseEntity<Object> deleteUser(@PathVariable Long id) {
        return ResponseEntity.ok(adminService.deleteUser(id));
    }

    @PatchMapping("/users/{id}/promote")
    public ResponseEntity<Object> promoteToAdmin(@PathVariable Long id) {
        return ResponseEntity.ok(adminService.promoteToAdmin(id));
    }

    @PatchMapping("/users/{id}/demote")
    public ResponseEntity<Object> demoteToUser(@PathVariable Long id) {
        return ResponseEntity.ok(adminService.demoteToUser(id));
    }

    // ─── WALLET ENDPOINTS ─────────────────────────────────────────────────────

    @GetMapping("/wallets")
    public List<Wallet> getAllWallets() {
        return adminService.getAllWallets();
    }

    @GetMapping("/wallets/{id}")
    public ResponseEntity<Object> getWalletById(@PathVariable Long id) {
        return ResponseEntity.ok(adminService.getWalletById(id));
    }

    // ─── TRANSACTION ENDPOINTS ────────────────────────────────────────────────

    /**
     * Now returns List<TransactionResponse> instead of List<Transaction>.
     *
     * Before: returned raw Transaction → frontend got senderWallet.user.name
     * After:  returns TransactionResponse → frontend gets senderName directly
     *
     * This fixes the empty From/To columns in the admin transactions table.
     */
    @GetMapping("/transactions")
    public List<TransactionResponse> getAllTransactions() {
        return adminService.getAllTransactions();
    }

    @GetMapping("/transactions/{id}")
    public ResponseEntity<Object> getTransactionById(@PathVariable Long id) {
        return ResponseEntity.ok(adminService.getTransactionById(id));
    }
}