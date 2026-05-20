package com.bright.wallet.exception;

/**
 * Custom exception thrown when someone tries to register
 * with an email that already exists in the database.
 *
 * Before this we got a 500 Internal Server Error because
 * PostgreSQL threw a unique constraint violation.
 *
 * Now we catch it in UserService before it hits the DB
 * and throw this exception — GlobalExceptionHandler catches it
 * and returns a clean 409 Conflict response.
 *
 * Lives in: com.bright.wallet.exception
 */
public class EmailAlreadyExistsException extends RuntimeException {

    public EmailAlreadyExistsException(String email) {
        super("An account with email '" + email + "' already exists.");
    }
}