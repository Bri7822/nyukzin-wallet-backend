package com.bright.wallet.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class EmailService {

    private final JavaMailSender mailSender;

    // Pulled from spring.mail.username in application.properties
    // so the From address is always in sync with the authenticated account
    @Value("${spring.mail.username}")
    private String fromAddress;

    public EmailService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    // =========================
    // EMAIL VERIFICATION
    // =========================
    public void sendVerificationEmail(String name, String email, String token) {

        String verifyLink = "http://localhost:5173/verify-email?token=" + token;

        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(fromAddress);
        message.setTo(email);
        message.setSubject("Nyukzin Wallet — Verify Your Email");
        message.setText(
                "Hi " + name + ",\n\n" +
                        "Welcome to Nyukzin Wallet! Please verify your email address " +
                        "to activate your account and start using the platform.\n\n" +
                        "Click the link below to verify:\n" +
                        verifyLink + "\n\n" +
                        "This link expires in 24 hours.\n\n" +
                        "If you did not create a Nyukzin account, you can safely ignore this email."
        );

        mailSender.send(message);
    }

    // =========================
    // RESET PASSWORD EMAIL
    // =========================
    public void sendResetEmail(String name, String email, String token) {

        String resetLink = "http://localhost:5173/reset-password?token=" + token;

        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(fromAddress);
        message.setTo(email);
        message.setSubject("Nyukzin Wallet — Password Reset");
        message.setText(
                "Hi " + name + ",\n\n" +
                        "We received a request to reset your password.\n" +
                        "Click the link below to set a new password:\n\n" +
                        resetLink + "\n\n" +
                        "This link expires in 1 hour.\n\n" +
                        "If you did not request this, you can safely ignore this email."
        );

        mailSender.send(message);
    }

    // =========================
    // PASSWORD CHANGED EMAIL
    // =========================
    public void sendPasswordChangedEmail(String name, String email) {

        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(fromAddress);
        message.setTo(email);
        message.setSubject("Nyukzin Wallet — Password Successfully Changed");
        message.setText(
                "Hi " + name + ",\n\n" +
                        "Your password was successfully updated.\n\n" +
                        "If this wasn't you, contact support immediately."
        );

        mailSender.send(message);
    }

    // =========================
    // WELCOME EMAIL
    // =========================
    public void sendWelcomeEmail(String name, String email) {

        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(fromAddress);
        message.setTo(email);
        message.setSubject("Welcome to Nyukzin Wallet!");
        message.setText(
                "Hi " + name + ",\n\n" +
                        "Your Nyukzin Wallet account is now active.\n\n" +
                        "You can log in and start managing your digital finances securely.\n\n" +
                        "If you did not create this account, contact support immediately."
        );

        mailSender.send(message);
    }
}