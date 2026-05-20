package com.bright.wallet.service;

import com.bright.wallet.dto.AdminRegisterRequest;
import com.bright.wallet.dto.RegisterResponse;
import com.bright.wallet.exception.EmailAlreadyExistsException;
import com.bright.wallet.model.User;
import com.bright.wallet.model.Wallet;
import com.bright.wallet.repository.UserRepository;
import com.bright.wallet.repository.WalletRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
public class UserService {

    private final UserRepository   userRepository;
    private final WalletRepository walletRepository;
    private final PasswordEncoder  passwordEncoder;
    private final EmailService     emailService;

    public UserService(UserRepository userRepository,
                       WalletRepository walletRepository,
                       PasswordEncoder passwordEncoder,
                       EmailService emailService) {
        this.userRepository   = userRepository;
        this.walletRepository = walletRepository;
        this.passwordEncoder  = passwordEncoder;
        this.emailService     = emailService;
    }

    public List<User> getUsers() {
        return userRepository.findAll();
    }

    // ─── REGISTER NORMAL USER ─────────────────────────────────────────────────

    public RegisterResponse registerUser(User user) {

        if (userRepository.existsByEmail(user.getEmail())) {
            throw new EmailAlreadyExistsException(user.getEmail());
        }

        user.setRole("ROLE_USER");
        user.setPassword(passwordEncoder.encode(user.getPassword()));

        // Account starts unverified — user must click the email link before logging in
        user.setEmailVerified(false);
        String verificationToken = UUID.randomUUID().toString();
        user.setVerificationToken(verificationToken);
        user.setVerificationTokenExpiry(LocalDateTime.now().plusHours(24));

        User savedUser = userRepository.save(user);

        Wallet wallet = new Wallet();
        wallet.setUser(savedUser);
        wallet.setBalance(BigDecimal.ZERO);
        Wallet savedWallet = walletRepository.save(wallet);

        emailService.sendVerificationEmail(
                savedUser.getName(),
                savedUser.getEmail(),
                verificationToken
        );

        return new RegisterResponse(
                savedUser.getId(),
                savedWallet.getId(),
                savedUser.getName(),
                savedUser.getEmail()
        );
    }

    // ─── VERIFY EMAIL ─────────────────────────────────────────────────────────

    public void verifyEmail(String token) {
        User user = userRepository.findByVerificationToken(token)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Invalid or expired verification link. Please request a new one."));

        if (user.getVerificationTokenExpiry() == null ||
                LocalDateTime.now().isAfter(user.getVerificationTokenExpiry())) {
            user.setVerificationToken(null);
            user.setVerificationTokenExpiry(null);
            userRepository.save(user);
            throw new IllegalArgumentException(
                    "Verification link has expired. Please request a new one.");
        }

        user.setEmailVerified(true);
        user.setVerificationToken(null);
        user.setVerificationTokenExpiry(null);
        userRepository.save(user);
    }

    // ─── RESEND VERIFICATION EMAIL ────────────────────────────────────────────

    public void resendVerification(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException(
                        "No account found with that email address."));

        if (user.isEmailVerified()) {
            throw new IllegalArgumentException(
                    "This account is already verified. You can log in.");
        }

        String newToken = UUID.randomUUID().toString();
        user.setVerificationToken(newToken);
        user.setVerificationTokenExpiry(LocalDateTime.now().plusHours(24));
        userRepository.save(user);

        emailService.sendVerificationEmail(
                user.getName(),
                user.getEmail(),
                newToken
        );
    }

    // ─── REGISTER ADMIN ───────────────────────────────────────────────────────

    /**
     * Admins are created by existing admins — trusted by definition.
     * Their accounts are pre-verified; no email verification step needed.
     */
    public RegisterResponse registerAdmin(AdminRegisterRequest request) {

        if (userRepository.existsByEmail(request.getEmail())) {
            throw new EmailAlreadyExistsException(request.getEmail());
        }

        User admin = new User();
        admin.setName(request.getName());
        admin.setEmail(request.getEmail());
        admin.setPassword(passwordEncoder.encode(request.getPassword()));
        admin.setRole("ROLE_ADMIN");
        admin.setEmailVerified(true); // admins skip email verification

        User savedAdmin = userRepository.save(admin);

        return new RegisterResponse(
                savedAdmin.getId(),
                null,
                savedAdmin.getName(),
                savedAdmin.getEmail()
        );
    }

    // ─── UPDATE / DELETE ──────────────────────────────────────────────────────

    public User updateUser(Long id, User updatedUser) {
        return userRepository.findById(id)
                .map(user -> {
                    if (!user.getEmail().equals(updatedUser.getEmail()) &&
                            userRepository.existsByEmail(updatedUser.getEmail())) {
                        throw new EmailAlreadyExistsException(updatedUser.getEmail());
                    }
                    user.setName(updatedUser.getName());
                    user.setEmail(updatedUser.getEmail());
                    user.setPassword(passwordEncoder.encode(updatedUser.getPassword()));
                    return userRepository.save(user);
                })
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + id));
    }

    public String deleteUser(Long id) {
        if (!userRepository.existsById(id)) {
            throw new IllegalArgumentException("User not found: " + id);
        }
        userRepository.deleteById(id);
        return "User deleted successfully";
    }
}