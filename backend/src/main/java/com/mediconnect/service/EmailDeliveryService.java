package com.mediconnect.service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;

@Service
public class EmailDeliveryService {
    private static final Logger log = LoggerFactory.getLogger(EmailDeliveryService.class);

    private final JavaMailSender mailSender;
    private final String host;
    private final String from;
    private final String frontendUrl;

    public EmailDeliveryService(JavaMailSender mailSender,
                                @Value("${spring.mail.host:}") String host,
                                @Value("${app.mail-from:}") String from,
                                @Value("${app.public-base-url:http://localhost:5173}") String frontendUrl) {
        this.mailSender = mailSender;
        this.host = host;
        this.from = from;
        this.frontendUrl = frontendUrl.replaceAll("/+$", "");
    }

    public boolean sendVerification(String email, String name, String token) {
        String link = frontendUrl + "/verify-email?token=" + token;
        String body = "Hello " + name + ",\n\nVerify your email to activate your Carevia account: "
                + link + "\n\nThis link expires in 24 hours. If you did not create this account, ignore this message.";
        return send(email, "Verify your Carevia email", body, "email verification");
    }

    public boolean sendPasswordReset(String email, String name, String token) {
        String link = frontendUrl + "/reset-password?token=" + token;
        String body = "Hello " + name + ",\n\nUse this link to reset your Carevia password: "
                + link + "\n\nThis link expires in 30 minutes. If you did not request a reset, ignore this message.";
        return send(email, "Reset your Carevia password", body, "password reset");
    }

    private boolean send(String recipient, String subject, String body, String purpose) {
        if (host == null || host.isBlank() || from == null || from.isBlank()) {
            log.warn("Carevia {} email was not sent because mail host or sender is not configured.", purpose);
            return false;
        }
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, false, StandardCharsets.UTF_8.name());
            helper.setFrom(from);
            helper.setTo(recipient);
            helper.setSubject(subject);
            helper.setText(body, false);
            mailSender.send(message);
            return true;
        } catch (MailException | MessagingException | IllegalArgumentException exception) {
            // Do not log recipient addresses, credentials, message bodies, or raw one-time tokens.
            log.warn("Carevia {} email delivery failed ({}). Check SMTP reachability and provider settings.",
                    purpose, exception.getClass().getSimpleName());
            return false;
        }
    }
}
