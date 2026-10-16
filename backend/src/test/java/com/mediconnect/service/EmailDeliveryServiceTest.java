package com.mediconnect.service;

import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.Test;
import org.springframework.mail.MailAuthenticationException;
import org.springframework.mail.javamail.JavaMailSender;

import java.util.Properties;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class EmailDeliveryServiceTest {
    @Test
    void verificationUsesConfiguredSpringMailSenderAndFrontendUrl() throws Exception {
        JavaMailSender sender = mock(JavaMailSender.class);
        MimeMessage message = new MimeMessage(Session.getInstance(new Properties()));
        when(sender.createMimeMessage()).thenReturn(message);
        EmailDeliveryService delivery = new EmailDeliveryService(sender, "localhost", "sender@example.test",
                "http://localhost:5173/");

        assertTrue(delivery.sendVerification("patient@example.test", "Test Patient", "opaque-token"));

        verify(sender).send(message);
        assertTrue(message.getContent().toString().contains("http://localhost:5173/verify-email?token=opaque-token"));
    }

    @Test
    void smtpAuthenticationFailureIsReportedAsNotDelivered() throws Exception {
        JavaMailSender sender = mock(JavaMailSender.class);
        when(sender.createMimeMessage()).thenReturn(new MimeMessage(Session.getInstance(new Properties())));
        doThrow(new MailAuthenticationException("provider rejected credentials")).when(sender).send(any(MimeMessage.class));
        EmailDeliveryService delivery = new EmailDeliveryService(sender, "smtp.example.test", "sender@example.test",
                "http://localhost:5173");

        assertFalse(delivery.sendPasswordReset("patient@example.test", "Test Patient", "opaque-token"));
    }
}
