package com.bright.wallet.exception;

/**
 * Thrown when a logged-in user tries to access or modify
 * a wallet that does not belong to them.
 *
 * Examples:
 * - User 1 tries to transfer FROM User 2's wallet
 * - User 1 tries to withdraw FROM User 2's wallet
 * - User 1 tries to check User 2's balance or history
 *
 * GlobalExceptionHandler catches this and returns 403 Forbidden.
 *
 * Lives in: com.bright.wallet.exception
 */
public class UnauthorizedAccessException extends RuntimeException {

    public UnauthorizedAccessException() {
        super("Access denied. This wallet does not belong to you.");
    }

    public UnauthorizedAccessException(String message) {
        super(message);
    }
}