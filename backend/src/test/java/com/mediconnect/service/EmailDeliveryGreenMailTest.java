package com.mediconnect.service;

import com.icegreen.greenmail.util.GreenMail;
import com.icegreen.greenmail.util.ServerSetup;
import org.junit.jupiter.api.Test;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import jakarta.mail.internet.MimeMessage;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EmailDeliveryGreenMailTest {
    @Test
    void verificationAndPasswordResetMessagesReachDevelopmentSmtpInbox() throws Exception {
        GreenMail inbox = new GreenMail(new ServerSetup(0, "127.0.0.1", ServerSetup.PROTOCOL_SMTP));
        inbox.start();
        try {
            JavaMailSenderImpl sender = new JavaMailSenderImpl();
            sender.setHost("127.0.0.1");
            sender.setPort(inbox.getSmtp().getPort());
            sender.setDefaultEncoding("UTF-8");
            EmailDeliveryService delivery = new EmailDeliveryService(sender, "127.0.0.1",
                    "no-reply@mediconnect.test", "http://localhost:5173");

            assertTrue(delivery.sendVerification("patient@example.test", "Synthetic Patient", "verify-token"));
            assertTrue(delivery.sendPasswordReset("patient@example.test", "Synthetic Patient", "reset-token"));
            assertTrue(inbox.waitForIncomingEmail(5000, 2));

            MimeMessage[] received = inbox.getReceivedMessages();
            assertEquals(2, received.length);
            assertTrue(received[0].getContent().toString().contains("/verify-email?token=verify-token"));
            assertTrue(received[1].getContent().toString().contains("/reset-password?token=reset-token"));
        } finally {
            inbox.stop();
        }
    }
}
