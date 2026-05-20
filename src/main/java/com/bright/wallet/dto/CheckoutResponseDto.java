package com.bright.wallet.dto;

public class CheckoutResponseDto {

    private String checkoutUrl;

    public CheckoutResponseDto(String checkoutUrl) {
        this.checkoutUrl = checkoutUrl;
    }

    public String getCheckoutUrl() { return checkoutUrl; }
}