package com.bright.wallet.dto;

/**
 * Returned after registration.
 * walletId is Long (not long) so it can be null for admin accounts.
 * Admin accounts have no wallet — frontend should check for null.
 */
public class RegisterResponse {

    private Long userId;
    private Long walletId;  // null for admin accounts
    private String name;
    private String email;
    private String message;

    public RegisterResponse(Long userId, Long walletId, String name, String email) {
        this.userId   = userId;
        this.walletId = walletId;
        this.name     = name;
        this.email    = email;
        this.message  = walletId != null
                ? "Registration successful"
                : "Admin account created successfully. No wallet assigned.";
    }

    public Long getUserId()   { return userId; }
    public Long getWalletId() { return walletId; }
    public String getName()   { return name; }
    public String getEmail()  { return email; }
    public String getMessage(){ return message; }
}