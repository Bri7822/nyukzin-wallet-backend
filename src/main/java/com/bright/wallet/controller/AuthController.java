package com.bright.wallet.controller;

import com.bright.wallet.dto.AdminRegisterRequest;
import com.bright.wallet.dto.AuthResponse;
import com.bright.wallet.dto.LoginRequest;
import com.bright.wallet.service.AuthService;
import com.bright.wallet.service.UserService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/auth")
public class AuthController {

    private final AuthService authService;
    private final UserService userService;

    public AuthController(AuthService authService, UserService userService) {
        this.authService = authService;
        this.userService = userService;
    }

    // ── LOGIN ─────────────────────────────────────────────────────────────────

    @PostMapping("/login")
    public ResponseEntity<Object> login(@Valid @RequestBody LoginRequest request) {
        try {
            AuthResponse response = authService.login(request);
            return ResponseEntity.ok(response);
        } catch (BadCredentialsException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Invalid email or password"));
        } catch (IllegalStateException e) {
            // Account exists and credentials are correct, but email is not verified.
            // 403 Forbidden is correct here — authenticated but not permitted yet.
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Login failed. Please try again."));
        }
    }

    // ── REGISTER ADMIN ────────────────────────────────────────────────────────

    @PostMapping("/register-admin")
    public ResponseEntity<Object> registerAdmin(
            @Valid @RequestBody AdminRegisterRequest request) {
        try {
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(userService.registerAdmin(request));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to create admin account."));
        }
    }

    // ── VERIFY EMAIL ──────────────────────────────────────────────────────────

    /**
     * GET /auth/verify-email?token=uuid-here
     *
     * The user clicks this link from their inbox.
     * The frontend's VerifyEmailView reads the token from the URL query param
     * and calls this endpoint. On success the account is activated and
     * the user can log in.
     */
    @GetMapping("/verify-email")
    public ResponseEntity<Object> verifyEmail(@RequestParam String token) {
        if (token == null || token.isBlank()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", "Verification token is required."));
        }
        try {
            userService.verifyEmail(token);
            return ResponseEntity.ok(
                    Map.of("message", "Email verified successfully. You can now log in."));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Verification failed. Please try again."));
        }
    }

    // ── RESEND VERIFICATION EMAIL ─────────────────────────────────────────────

    /**
     * POST /auth/resend-verification
     *
     * Request body: { "email": "user@example.com" }
     *
     * Used when a user's first verification link expired or was lost.
     * Issues a fresh 24-hour token and resends the email.
     *
     * Always returns 200 even if the email isn't found — prevents account enumeration.
     */
    @PostMapping("/resend-verification")
    public ResponseEntity<Object> resendVerification(
            @RequestBody Map<String, String> body) {
        String email = body.get("email");
        if (email == null || email.isBlank()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", "Email is required."));
        }
        try {
            userService.resendVerification(email);
            return ResponseEntity.ok(
                    Map.of("message", "Verification email sent. Please check your inbox."));
        } catch (IllegalArgumentException e) {
            // Return a generic message to prevent account enumeration —
            // an attacker shouldn't be able to confirm whether an email exists
            return ResponseEntity.ok(
                    Map.of("message", "If that email is registered and unverified, we've sent a new link."));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to resend verification. Please try again."));
        }
    }

    // ── FORGOT PASSWORD ───────────────────────────────────────────────────────

    @PostMapping("/forgot-password")
    public ResponseEntity<Object> forgotPassword(
            @RequestBody Map<String, String> body) {
        String email = body.get("email");
        if (email == null || email.isBlank()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", "Email is required."));
        }
        try {
            Map<String, String> result = authService.forgotPassword(email);
            return ResponseEntity.ok(result);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to process request. Please try again."));
        }
    }

    // ── RESET PASSWORD ────────────────────────────────────────────────────────

    @PostMapping("/reset-password")
    public ResponseEntity<Object> resetPassword(
            @RequestBody Map<String, String> body) {
        String token       = body.get("token");
        String newPassword = body.get("newPassword");

        if (token == null || token.isBlank()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", "Reset token is required."));
        }
        if (newPassword == null || newPassword.length() < 6) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", "Password must be at least 6 characters."));
        }
        try {
            authService.resetPassword(token, newPassword);
            return ResponseEntity.ok(
                    Map.of("message", "Password reset successfully. Please login."));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to reset password. Please try again."));
        }
    }
}