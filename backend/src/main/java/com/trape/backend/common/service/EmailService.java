package com.trape.backend.common.service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class EmailService {

    private static final Logger log = LoggerFactory.getLogger(EmailService.class);

    private final JavaMailSender mailSender;
    private final String apiKey;
    private final String senderEmail;
    private final String senderName;
    private final String apiUrl;
    private final String smtpUsername;
    private final RestClient restClient;

    public EmailService(
            @Autowired(required = false) JavaMailSender mailSender,
            @Value("${brevo.api.key:}") String apiKey,
            @Value("${brevo.sender.email:}") String senderEmail,
            @Value("${brevo.sender.name:Trape}") String senderName,
            @Value("${brevo.api.url:https://api.brevo.com/v3/smtp/email}") String apiUrl,
            @Value("${spring.mail.username:}") String smtpUsername) {
        this.mailSender = mailSender;
        this.apiKey = apiKey != null ? apiKey.trim() : "";
        this.smtpUsername = smtpUsername != null ? smtpUsername.trim() : "";

        // Use BREVO_SENDER_EMAIL, or fallback to SMTP_USERNAME if it's formatted as an email
        String resolvedSenderEmail = (senderEmail != null && !senderEmail.isBlank())
                ? senderEmail.trim()
                : this.smtpUsername;
        this.senderEmail = resolvedSenderEmail;

        this.senderName = (senderName != null && !senderName.isBlank()) ? senderName.trim() : "Trape";
        this.apiUrl = (apiUrl != null && !apiUrl.isBlank()) ? apiUrl.trim() : "https://api.brevo.com/v3/smtp/email";

        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(10000);
        requestFactory.setReadTimeout(15000);

        this.restClient = RestClient.builder()
                .requestFactory(requestFactory)
                .baseUrl(this.apiUrl)
                .build();

        if (isBrevoApiConfigured()) {
            log.info("EmailService initialized with Brevo REST API (Sender: '{}' <{}>)", this.senderName, this.senderEmail);
        } else if (isSmtpConfigured()) {
            log.info("EmailService initialized with SMTP relay fallback (User: {})", this.smtpUsername);
        } else {
            log.warn("EmailService initialized without credentials. Emails will not be sent until BREVO_API_KEY and BREVO_SENDER_EMAIL are configured.");
        }
    }

    @Async
    public void sendSimpleEmail(String to, String subject, String body) {
        sendEmail(to, null, subject, body, null);
    }

    @Async
    public void sendHtmlEmail(String to, String subject, String htmlBody) {
        sendEmail(to, null, subject, null, htmlBody);
    }

    @Async
    public void sendEmail(String to, String toName, String subject, String textBody, String htmlBody) {
        if (!isConfigured()) {
            log.warn("Brevo email service credentials are not configured. Email to '{}' with subject '{}' was not sent.", to, subject);
            return;
        }

        if (isBrevoApiConfigured()) {
            sendViaBrevoApi(to, toName, subject, textBody, htmlBody);
        } else if (isSmtpConfigured()) {
            sendViaSmtp(to, subject, textBody, htmlBody);
        }
    }

    private void sendViaBrevoApi(String to, String toName, String subject, String textBody, String htmlBody) {
        Map<String, Object> payload = new HashMap<>();

        Map<String, String> sender = new HashMap<>();
        sender.put("email", senderEmail);
        if (senderName != null && !senderName.isBlank()) {
            sender.put("name", senderName);
        }
        payload.put("sender", sender);

        Map<String, String> recipient = new HashMap<>();
        recipient.put("email", to);
        if (toName != null && !toName.isBlank()) {
            recipient.put("name", toName);
        }
        payload.put("to", List.of(recipient));

        payload.put("subject", subject);
        if (htmlBody != null && !htmlBody.isBlank()) {
            payload.put("htmlContent", htmlBody);
        }
        if (textBody != null && !textBody.isBlank()) {
            payload.put("textContent", textBody);
        }

        try {
            ResponseEntity<String> response = restClient.post()
                    .header("api-key", apiKey)
                    .contentType(MediaType.APPLICATION_JSON)
                    .accept(MediaType.APPLICATION_JSON)
                    .body(payload)
                    .retrieve()
                    .toEntity(String.class);

            log.info("Email successfully sent via Brevo API to '{}' (Subject: '{}', Status: {})", to, subject, response.getStatusCode());
        } catch (RestClientResponseException e) {
            log.error("Brevo API returned error {} while sending email to '{}': {}",
                    e.getStatusCode(), to, e.getResponseBodyAsString());
        } catch (Exception e) {
            log.error("Failed to send email to '{}' via Brevo API: {}", to, e.getMessage(), e);
        }
    }

    private void sendViaSmtp(String to, String subject, String textBody, String htmlBody) {
        try {
            if (htmlBody != null && !htmlBody.isBlank()) {
                MimeMessage message = mailSender.createMimeMessage();
                MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
                helper.setFrom(senderEmail);
                helper.setTo(to);
                helper.setSubject(subject);
                helper.setText(htmlBody, true);
                mailSender.send(message);
                log.info("HTML email successfully sent via SMTP to {}", to);
            } else {
                SimpleMailMessage message = new SimpleMailMessage();
                message.setFrom(senderEmail);
                message.setTo(to);
                message.setSubject(subject);
                message.setText(textBody != null ? textBody : "");
                mailSender.send(message);
                log.info("Plain-text email successfully sent via SMTP to {}", to);
            }
        } catch (MessagingException e) {
            log.error("Failed to compose/send HTML email to {} via SMTP: {}", to, e.getMessage(), e);
        } catch (Exception e) {
            log.error("Unexpected error sending email to {} via SMTP: {}", to, e.getMessage(), e);
        }
    }

    @Async
    public void sendOrderConfirmation(String to, String customerName, String orderNumber, BigDecimal totalAmount) {
        String name = (customerName != null && !customerName.isBlank()) ? customerName : "Customer";
        String subject = "Order Confirmed - #" + orderNumber + " | Trape";

        String textBody = String.format(
                "Hello %s,\n\n" +
                "Thank you for shopping with Trape! Your order #%s has been successfully placed.\n\n" +
                "Total Amount: ₹%.2f\n\n" +
                "We will notify you once your order is shipped.\n\n" +
                "Warm regards,\n" +
                "The Trape Team",
                name,
                orderNumber,
                totalAmount
        );

        String htmlBody = String.format("""
                <div style="font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Helvetica, Arial, sans-serif; max-width: 600px; margin: 0 auto; padding: 24px; border: 1px solid #e2e8f0; border-radius: 8px; background-color: #ffffff;">
                    <div style="text-align: center; padding-bottom: 20px; border-bottom: 1px solid #edf2f7;">
                        <h1 style="color: #111827; margin: 0; font-size: 26px; font-weight: 800; letter-spacing: -0.5px;">Trape</h1>
                    </div>
                    <div style="padding: 24px 0;">
                        <h2 style="color: #1f2937; margin-top: 0; font-size: 20px;">Order Confirmed!</h2>
                        <p style="color: #4b5563; font-size: 15px; line-height: 1.6;">Hello <strong>%s</strong>,</p>
                        <p style="color: #4b5563; font-size: 15px; line-height: 1.6;">Thank you for shopping with Trape! Your order has been successfully placed and paid.</p>
                        <div style="background-color: #f9fafb; border: 1px solid #e5e7eb; border-radius: 6px; padding: 18px; margin: 20px 0;">
                            <p style="margin: 0 0 6px 0; color: #6b7280; font-size: 13px; text-transform: uppercase; font-weight: 600;">Order Number</p>
                            <p style="margin: 0 0 16px 0; color: #111827; font-size: 18px; font-weight: 700; font-family: monospace;">#%s</p>
                            <p style="margin: 0 0 6px 0; color: #6b7280; font-size: 13px; text-transform: uppercase; font-weight: 600;">Total Paid</p>
                            <p style="margin: 0; color: #059669; font-size: 22px; font-weight: 800;">₹%.2f</p>
                        </div>
                        <p style="color: #4b5563; font-size: 15px; line-height: 1.6;">We are currently processing your order and will notify you as soon as it ships.</p>
                    </div>
                    <div style="border-top: 1px solid #edf2f7; padding-top: 20px; text-align: center; color: #9ca3af; font-size: 13px;">
                        <p style="margin: 0;">Warm regards,<br><strong style="color: #6b7280;">The Trape Team</strong></p>
                    </div>
                </div>
                """,
                name,
                orderNumber,
                totalAmount
        );

        sendEmail(to, name, subject, textBody, htmlBody);
    }

    public boolean isConfigured() {
        return isBrevoApiConfigured() || isSmtpConfigured();
    }

    public boolean isBrevoApiConfigured() {
        return apiKey != null && !apiKey.isBlank() && senderEmail != null && !senderEmail.isBlank();
    }

    public boolean isSmtpConfigured() {
        return mailSender != null && smtpUsername != null && !smtpUsername.isBlank();
    }

    public String getSenderEmail() {
        return senderEmail;
    }

    public String getSenderName() {
        return senderName;
    }
}
