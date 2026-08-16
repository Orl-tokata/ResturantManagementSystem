package com.resturant.management.rms.auth;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * Sends the password-reset OTP.
 *
 * <p>When no SMTP username is configured the code is logged instead of sent, so
 * the reset flow is still testable on a developer machine without wiring up a
 * mail account. That fallback is deliberately loud.
 */
@Slf4j
@Service
public class MailService {

    private final JavaMailSender mailSender;
    private final String from;
    private final boolean configured;

    public MailService(JavaMailSender mailSender,
                       @Value("${spring.mail.username:}") String from) {
        this.mailSender = mailSender;
        this.from = from;
        this.configured = StringUtils.hasText(from);
    }

    public void sendOtp(String to, String code, int validMinutes) {
        String subject = "Password reset code / លេខកូដកំណត់ពាក្យសម្ងាត់ឡើងវិញ";
        String body = """
                Your password reset code is: %s

                It expires in %d minutes. If you did not request this, ignore this email.

                លេខកូដកំណត់ពាក្យសម្ងាត់របស់អ្នកគឺ៖ %s
                វានឹងផុតកំណត់ក្នុងរយៈពេល %d នាទី។
                """.formatted(code, validMinutes, code, validMinutes);

        if (!configured) {
            log.warn("""

                    ================================================================
                     MAIL NOT CONFIGURED — password reset code not sent by email.
                       to   : {}
                       code : {}
                     Set MAIL_USERNAME / MAIL_PASSWORD to enable real delivery.
                    ================================================================
                    """, to, code);
            return;
        }

        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(from);
            message.setTo(to);
            message.setSubject(subject);
            message.setText(body);
            mailSender.send(message);
            log.info("Password reset code sent to {}", mask(to));
        } catch (Exception e) {
            // Never surface SMTP details to the caller — that leaks configuration.
            log.error("Failed to send password reset email to {}", mask(to), e);
        }
    }

    private String mask(String email) {
        int at = email.indexOf('@');
        if (at <= 2) return "***";
        return email.charAt(0) + "***" + email.substring(at - 1);
    }
}
