package com.bright.wallet.dto;

import com.bright.wallet.model.Wallet;

/**
 * Safe wallet info returned in API responses.
 * Only exposes what the client needs — never password or role.
 */
public class WalletResponse {

    private Long walletId;
    private Long userId;
    private String ownerName;  // name only — not email, not password, not role

    public WalletResponse(Wallet wallet) {
        this.walletId  = wallet.getId();
        this.userId    = wallet.getUser().getId();
        this.ownerName = wallet.getUser().getName();
    }

    public Long getWalletId()    { return walletId; }
    public Long getUserId()      { return userId; }
    public String getOwnerName() { return ownerName; }
}