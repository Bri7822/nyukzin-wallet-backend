package com.bright.wallet.repository;

import com.bright.wallet.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);

    // Used in AuthService.resetPassword() to validate the password reset token
    Optional<User> findByResetToken(String resetToken);

    // Used in UserService.verifyEmail() to validate the email verification token
    Optional<User> findByVerificationToken(String verificationToken);
}