package com.trape.backend.common.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

class EmailServiceTest {

    @Test
    @DisplayName("EmailService reports unconfigured when keys are missing")
    void unconfiguredWhenKeysMissing() {
        EmailService service = new EmailService(
                null,
                "",
                "",
                "Trape",
                "https://api.brevo.com/v3/smtp/email",
                ""
        );

        assertFalse(service.isConfigured());
        assertFalse(service.isBrevoApiConfigured());
        assertFalse(service.isSmtpConfigured());

        // Should safely no-op without throwing
        assertDoesNotThrow(() -> service.sendSimpleEmail("test@example.com", "Test", "Body"));
        assertDoesNotThrow(() -> service.sendHtmlEmail("test@example.com", "Test", "<p>Body</p>"));
        assertDoesNotThrow(() -> service.sendOrderConfirmation("test@example.com", "John Doe", "TRP-12345", BigDecimal.valueOf(999.00)));
    }

    @Test
    @DisplayName("EmailService reports configured when Brevo API key and sender email are present")
    void configuredWithBrevoApi() {
        EmailService service = new EmailService(
                null,
                "xkeysib-test-api-key",
                "noreply@trape.com",
                "Trape Store",
                "https://api.brevo.com/v3/smtp/email",
                ""
        );

        assertTrue(service.isConfigured());
        assertTrue(service.isBrevoApiConfigured());
        assertFalse(service.isSmtpConfigured());
        assertEquals("noreply@trape.com", service.getSenderEmail());
        assertEquals("Trape Store", service.getSenderName());
    }
}
