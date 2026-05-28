package com.bright.wallet.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
public class EmailService {

    private final JavaMailSender mailSender;

    @Value("${frontend.url:http://localhost:5173}")
    private String frontendUrl;

    @Value("${spring.mail.username}")
    private String fromAddress;

    public EmailService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    // =========================
    // EMAIL VERIFICATION
    // =========================
    public void sendVerificationEmail(String name, String email, String token) {
        String verifyLink = frontendUrl + "/verify-email?token=" + token;

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
        String resetLink = frontendUrl + "/reset-password?token=" + token;

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

    // =========================
    // TRANSFER SENT (debit)
    // Notify the sender that their transfer went through
    // and show their updated balance.
    // =========================
    public void sendTransferSentEmail(String senderName,
                                      String senderEmail,
                                      String receiverName,
                                      BigDecimal amount,
                                      BigDecimal newBalance) {

        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(fromAddress);
        message.setTo(senderEmail);
        message.setSubject("Nyukzin Wallet — Transfer Sent");
        message.setText(
                "Hi " + senderName + ",\n\n" +
                        "Your transfer was successful.\n\n" +
                        "  Amount sent  : R " + String.format("%.2f", amount) + "\n" +
                        "  Recipient    : " + receiverName + "\n" +
                        "  New balance  : R " + String.format("%.2f", newBalance) + "\n\n" +
                        "If you did not authorise this transfer, please contact support immediately.\n\n" +
                        "— Nyukzin Wallet"
        );

        mailSender.send(message);
    }

    // =========================
    // TRANSFER RECEIVED (credit)
    // Notify the receiver that funds have landed
    // and show their updated balance.
    // =========================
    public void sendTransferReceivedEmail(String receiverName,
                                          String receiverEmail,
                                          String senderName,
                                          BigDecimal amount,
                                          BigDecimal newBalance) {

        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(fromAddress);
        message.setTo(receiverEmail);
        message.setSubject("Nyukzin Wallet — Funds Received");
        message.setText(
                "Hi " + receiverName + ",\n\n" +
                        "You have received a transfer!\n\n" +
                        "  Amount received : R " + String.format("%.2f", amount) + "\n" +
                        "  Sent by         : " + senderName + "\n" +
                        "  New balance     : R " + String.format("%.2f", newBalance) + "\n\n" +
                        "Log in to your Nyukzin Wallet to view your updated balance.\n\n" +
                        "— Nyukzin Wallet"
        );

        mailSender.send(message);
    }

    // =========================
// WITHDRAWAL CONFIRMATION
// Notify the user their withdrawal went through
// and show their updated balance.
// =========================
    public void sendWithdrawalEmail(String name,
                                    String email,
                                    BigDecimal amount,
                                    BigDecimal newBalance) {

        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(fromAddress);
        message.setTo(email);
        message.setSubject("Nyukzin Wallet — Withdrawal Confirmed");
        message.setText(
                "Hi " + name + ",\n\n" +
                        "Your withdrawal was processed successfully.\n\n" +
                        "  Amount withdrawn : R " + String.format("%.2f", amount) + "\n" +
                        "  Remaining balance: R " + String.format("%.2f", newBalance) + "\n\n" +
                        "If you did not authorise this withdrawal, please contact support immediately.\n\n" +
                        "— Nyukzin Wallet"
        );

        mailSender.send(message);
    }
}

