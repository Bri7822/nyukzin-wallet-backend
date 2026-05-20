package com.bright.wallet.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;

@Entity
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Name is required")
    @Size(min = 2, max = 100, message = "Name must be between 2 and 100 characters")
    private String name;

    @NotBlank(message = "Email is required")
    @Email(message = "Email must be a valid email address")
    @Column(unique = true)
    private String email;

    @NotBlank(message = "Password is required")
    @Size(min = 6, message = "Password must be at least 6 characters")
    private String password;

    private String role = "ROLE_USER";

    private LocalDateTime createdAt = LocalDateTime.now();

    // ── Password Reset ─────────────────────────────────────────────────────
    // resetToken       : UUID generated when user requests a password reset
    // resetTokenExpiry : timestamp after which the token is invalid (1 hour)
    // Both are cleared after a successful reset
    @Column(name = "reset_token")
    private String resetToken;

    @Column(name = "reset_token_expiry")
    private LocalDateTime resetTokenExpiry;

    // ── Email Verification ─────────────────────────────────────────────────
    // emailVerified          : false until the user clicks their verification link
    // verificationToken      : UUID sent in the welcome email
    // verificationTokenExpiry: link expires after 24 hours
    // Token is cleared and emailVerified set to true after successful verification
    @Column(name = "email_verified", nullable = false)
    private boolean emailVerified = false;

    @Column(name = "verification_token")
    private String verificationToken;

    @Column(name = "verification_token_expiry")
    private LocalDateTime verificationTokenExpiry;

    @OneToOne(mappedBy = "user", cascade = CascadeType.ALL)
    private Wallet wallet;

    public User() {}

    public User(String name, String email, String password) {
        this.name     = name;
        this.email    = email;
        this.password = password;
    }

    // ── Getters & Setters ──────────────────────────────────────────────────

    public Long getId()                            { return id; }

    public String getName()                        { return name; }
    public void   setName(String name)             { this.name = name; }

    public String getEmail()                       { return email; }
    public void   setEmail(String email)           { this.email = email; }

    public String getPassword()                    { return password; }
    public void   setPassword(String password)     { this.password = password; }

    public String getRole()                        { return role; }
    public void   setRole(String role)             { this.role = role; }

    public LocalDateTime getCreatedAt()            { return createdAt; }

    public String getResetToken()                  { return resetToken; }
    public void   setResetToken(String t)          { this.resetToken = t; }

    public LocalDateTime getResetTokenExpiry()     { return resetTokenExpiry; }
    public void   setResetTokenExpiry(LocalDateTime e) { this.resetTokenExpiry = e; }

    public boolean isEmailVerified()               { return emailVerified; }
    public void    setEmailVerified(boolean v)     { this.emailVerified = v; }

    public String getVerificationToken()           { return verificationToken; }
    public void   setVerificationToken(String t)   { this.verificationToken = t; }

    public LocalDateTime getVerificationTokenExpiry()          { return verificationTokenExpiry; }
    public void          setVerificationTokenExpiry(LocalDateTime e) { this.verificationTokenExpiry = e; }
}