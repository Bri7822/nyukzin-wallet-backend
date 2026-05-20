package com.bright.wallet.dto;

/**
 * Returned after successful login.
 * Now includes role so the frontend knows
 * whether to show the admin dashboard or the user dashboard.
 */
public class AuthResponse {

    private String token;
    private Long userId;
    private Long walletId;
    private String name;
    private String email;
    private String role;

    public AuthResponse(String token, Long userId, Long walletId,
                        String name, String email, String role) {
        this.token    = token;
        this.userId   = userId;
        this.walletId = walletId;
        this.name     = name;
        this.email    = email;
        this.role     = role;
    }

    public String getToken()    { return token; }
    public Long getUserId()     { return userId; }
    public Long getWalletId()   { return walletId; }
    public String getName()     { return name; }
    public String getEmail()    { return email; }
    public String getRole()     { return role; }
}