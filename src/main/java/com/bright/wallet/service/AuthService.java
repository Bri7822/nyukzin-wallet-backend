package com.bright.wallet.service;

import com.bright.wallet.dto.AuthResponse;
import com.bright.wallet.dto.LoginRequest;
import com.bright.wallet.model.User;
import com.bright.wallet.model.Wallet;
import com.bright.wallet.repository.UserRepository;
import com.bright.wallet.repository.WalletRepository;
import com.bright.wallet.security.JwtUtil;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

@Service
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final UserRepository        userRepository;
    private final WalletRepository      walletRepository;
    private final JwtUtil               jwtUtil;
    private final PasswordEncoder       passwordEncoder;
    private final EmailService          emailService;

    public AuthService(AuthenticationManager authenticationManager,
                       UserRepository userRepository,
                       WalletRepository walletRepository,
                       JwtUtil jwtUtil,
                       PasswordEncoder passwordEncoder,
                       EmailService emailService) {
        this.authenticationManager = authenticationManager;
        this.userRepository        = userRepository;
        this.walletRepository      = walletRepository;
        this.jwtUtil               = jwtUtil;
        this.passwordEncoder       = passwordEncoder;
        this.emailService          = emailService;
    }

    // ── LOGIN ─────────────────────────────────────────────────────────────────

    public AuthResponse login(LoginRequest request) {
        // Spring Security validates credentials first — throws BadCredentialsException
        // if email/password are wrong, before we even reach the verified check
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.getEmail(), request.getPassword()));

        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        // Block login until the user has clicked their verification link.
        // This fires AFTER credential validation — a user who never verified
        // but tries wrong passwords still gets "Invalid email or password",
        // not a hint that the account exists.
        if (!user.isEmailVerified()) {
            throw new IllegalStateException(
                    "Please verify your email before logging in. " +
                            "Check your inbox or request a new verification link.");
        }

        Wallet wallet = walletRepository.findByUser(user)
                .orElseThrow(() -> new IllegalArgumentException("Wallet not found"));

        String token = jwtUtil.generateToken(user.getEmail(), user.getRole());

        return new AuthResponse(
                token, user.getId(), wallet.getId(),
                user.getName(), user.getEmail(), user.getRole());
    }

    // ── FORGOT PASSWORD ───────────────────────────────────────────────────────

    public Map<String, String> forgotPassword(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException(
                        "No account found with that email address."));

        String token         = UUID.randomUUID().toString();
        LocalDateTime expiry = LocalDateTime.now().plusHours(1);

        user.setResetToken(token);
        user.setResetTokenExpiry(expiry);
        userRepository.save(user);

        emailService.sendResetEmail(
                user.getName(),
                user.getEmail(),
                token
        );

        return Map.of(
                "token",     token,
                "userName",  user.getName(),
                "userEmail", user.getEmail(),
                "expiresAt", expiry.toString()
        );
    }

    // ── RESET PASSWORD ────────────────────────────────────────────────────────

    public void resetPassword(String token, String newPassword) {
        User user = userRepository.findByResetToken(token)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Invalid or expired reset link. Please request a new one."));

        if (user.getResetTokenExpiry() == null ||
                LocalDateTime.now().isAfter(user.getResetTokenExpiry())) {
            user.setResetToken(null);
            user.setResetTokenExpiry(null);
            userRepository.save(user);
            throw new IllegalArgumentException(
                    "Reset link has expired. Please request a new one.");
        }

        user.setPassword(passwordEncoder.encode(newPassword));
        user.setResetToken(null);
        user.setResetTokenExpiry(null);
        userRepository.save(user);

        // "Password changed" notification fires here, AFTER the password is saved
        emailService.sendPasswordChangedEmail(
                user.getName(),
                user.getEmail()
        );
    }
}